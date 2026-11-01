import assert from 'node:assert/strict';
import {after, before, test} from 'node:test';
import {createServer} from 'vite';
import {createElement as h} from 'react';
import {renderToStaticMarkup} from 'react-dom/server';
import {Provider} from 'react-redux';
import {MemoryRouter, Routes, Route} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {incidentKeys} from '@/modules/incidents/query/incidentKeys.ts';
import {readIncidentParams} from '@/modules/incidents/utils/incidentList.ts';

let server, IncidentsPage, IncidentDetailPage, store, setAuth, apiClient, api;
const incident = {
    id: 'incident-1', incidentNumber: 'INC-001', title: 'Database timeout', description: 'Investigate connection pool',
    status: 'OPEN', severity: 'SEV1', createdBy: 'user-1', createdByName: 'Alice Smith', createdAt: '2026-10-31T10:00:00Z', updatedAt: '2026-10-31T10:00:00Z', version: 0,
};
const page = {items: [incident], page: 0, size: 20, totalItems: 1, totalPages: 1, first: true, last: true};

before(async () => {
    server = await createServer({server: {middlewareMode: true, hmr: false}, appType: 'custom'});
    ({IncidentsPage} = await server.ssrLoadModule('/src/modules/incidents/pages/IncidentsPage.tsx'));
    ({IncidentDetailPage} = await server.ssrLoadModule('/src/modules/incidents/pages/IncidentDetailPage.tsx'));
    ({store} = await server.ssrLoadModule('/src/app/store/store.ts'));
    ({setAuth} = await server.ssrLoadModule('/src/modules/auth/store/authSlice.ts'));
    ({apiClient} = await server.ssrLoadModule('/src/shared/api/apiClient.ts'));
    api = await server.ssrLoadModule('/src/modules/incidents/api/incidentApi.ts');
});
after(async () => { await server?.close(); });

async function render({permissions = ['incident:view'], roles = [], data, url = '/incidents', error = false} = {}) {
    store.dispatch(setAuth({accessToken: 'test-token', user: {id: 'user-1', organizationId: 'org-1', permissions, roles}}));
    const client = new QueryClient({defaultOptions: {queries: {retry: false, retryOnMount: false, staleTime: Infinity}}});
    const key = incidentKeys.list(readIncidentParams(new URL(url, 'https://opsflow.test').searchParams));
    if (data) client.setQueryData(key, data);
    if (error) await client.fetchQuery({queryKey: key, queryFn: () => Promise.reject(new Error('offline'))}).catch(() => {});
    try {
        return renderToStaticMarkup(h(Provider, {store}, h(QueryClientProvider, {client}, h(MemoryRouter, {initialEntries: [url]}, h(IncidentsPage)))));
    } finally {
        client.clear();
    }
}

test('incident page renders loading, retryable error and distinct empty states', async () => {
    assert.match(await render(), /Loading incidents/);
    assert.match(await render({error: true}), /Failed to load incidents.*Retry/s);
    const empty = {...page, items: [], totalItems: 0, totalPages: 0, last: true};
    assert.match(await render({data: empty}), /No incidents yet/);
    assert.match(await render({data: empty, url: '/incidents?q=missing&status=OPEN'}), /No matching incidents/);
});

test('viewers can see incidents and pagination without mutation actions', async () => {
    const html = await render({data: page});
    assert.match(html, /INC-001/);
    assert.match(html, /Database timeout/);
    assert.match(html, /Investigate connection pool/);
    assert.match(html, /Page 1 of 1/);
    assert.match(html, /disabled="">Previous/);
    assert.match(html, /disabled="">Next/);
});

test('update, resolve and administrator permissions gate rendered actions', async () => {
    const editor = await render({data: page, permissions: ['incident:view', 'incident:update']});
    assert.match(editor, /Acknowledge INC-001/);
    const mitigated = {...page, items: [{...incident, status: 'MITIGATED'}]};
    assert.doesNotMatch(await render({data: mitigated, permissions: ['incident:update']}), /Resolve INC-001/);
    assert.match(await render({data: mitigated, permissions: ['incident:resolve']}), /Resolve INC-001/);
    const admin = await render({data: page, permissions: [], roles: ['ROLE_PLATFORM_ADMIN']});
    assert.match(admin, /Acknowledge INC-001/);
    assert.doesNotMatch(await render({data: {...page, items: [{...incident, status: 'CLOSED'}]}, roles: ['ROLE_PLATFORM_ADMIN']}), /aria-label="(?:Close|Resolve|Acknowledge) INC-001"/);
});

test('API sends backend search parameters and adapts Spring PagedModel metadata', async () => {
    const original = apiClient.defaults.adapter;
    const signal = new AbortController().signal;
    const params = readIncidentParams(new URLSearchParams('q=timeout&page=2&size=10&status=OPEN&severity=SEV1'));
    try {
        apiClient.defaults.adapter = async config => {
            assert.equal(config.url, '/incidents');
            assert.equal(config.method, 'get');
            assert.deepEqual(config.params, params);
            assert.equal(config.signal, signal);
            return {config, headers: {}, status: 200, statusText: 'OK', data: {
                content: [incident], page: {number: 2, size: 10, totalElements: 21, totalPages: 3},
            }};
        };
        assert.deepEqual(await api.getIncidents(params, signal), {
            items: [incident], page: 2, size: 10, totalItems: 21, totalPages: 3, first: false, last: true,
        });
    } finally {
        apiClient.defaults.adapter = original;
    }
});

test('lifecycle actions call the existing backend endpoints', async () => {
    const original = apiClient.defaults.adapter;
    const calls = [];
    try {
        apiClient.defaults.adapter = async config => {
            calls.push({url: config.url, method: config.method, body: config.data});
            return {config, headers: {}, status: 200, statusText: 'OK', data: incident};
        };
        for (const action of ['acknowledge', 'start-investigation', 'mitigate', 'resolve', 'close']) {
            assert.deepEqual(await api.transitionIncident(incident.id, action), incident);
        }
        assert.deepEqual(calls.map(call => call.url), ['acknowledge', 'start-investigation', 'mitigate', 'resolve', 'close'].map(action => `/incidents/incident-1/${action}`));
        assert.ok(calls.every(call => call.method === 'post'));
    } finally {
        apiClient.defaults.adapter = original;
    }
});

async function renderDetail({permissions = ['incident:view'], status = 'OPEN', loading = false} = {}) {
    store.dispatch(setAuth({accessToken: 'test-token', user: {id: 'user-1', organizationId: 'org-1', permissions, roles: []}}));
    const client = new QueryClient({defaultOptions: {queries: {retry: false, staleTime: Infinity}}});
    const history = {content: [], page: {number: 0, size: 20, totalElements: 0, totalPages: 0}};
    if (!loading) client.setQueryData(incidentKeys.detail(incident.id), {...incident, status});
    client.setQueryData(incidentKeys.timeline(incident.id, 0), {...history, content: [{id: 'event-1', eventType: 'INCIDENT_CREATED', actorId: 'user-1', actorName: 'Alice Smith', createdAt: incident.createdAt, oldValue: null, newValue: 'OPEN'}]});
    client.setQueryData(incidentKeys.comments(incident.id, 0), {...history, content: [
        {id: 'comment-1', userId: 'user-1', comment: 'My investigation notes', version: 2, createdAt: incident.createdAt},
        {id: 'comment-2', userId: 'user-2', comment: 'Another investigator', version: 0, createdAt: incident.createdAt},
    ]});
    for (const resource of ['teams', 'users']) client.setQueryData(['incident-directory', resource], []);
    try {
        return renderToStaticMarkup(h(Provider, {store}, h(QueryClientProvider, {client}, h(MemoryRouter, {initialEntries: [`/incidents/${incident.id}`]}, h(Routes, {}, h(Route, {path: '/incidents/:id', element: h(IncidentDetailPage)}))))));
    } finally { client.clear(); }
}

test('detail query renders header, version, assignment, timeline and comments for viewers', async () => {
    assert.match(await renderDetail({loading: true}), /Loading incident/);
    const html = await renderDetail();
    for (const text of ['INC-001', 'Database timeout', 'SEV1', 'Version 0', 'Assigned team', 'Unassigned', 'incident created', 'My investigation notes']) assert.ok(html.includes(text), text);
    assert.doesNotMatch(html, /Edit details|Edit comment|Post comment|Assign team|>Acknowledge</);
    assert.match(html, /By Alice Smith/);
    assert.match(html, /<dt>Created by<\/dt><dd>Alice Smith<\/dd>/);
    assert.doesNotMatch(html, /By user-1/);
});

test('detail actions respect update, assign, resolve and comment ownership', async () => {
    const editor = await renderDetail({permissions: ['incident:view', 'incident:update']});
    assert.match(editor, /Edit details/);
    assert.match(editor, /Post comment/);
    assert.match(editor, />Acknowledge</);
    assert.equal((editor.match(/>Edit comment</g) || []).length, 1);
    assert.doesNotMatch(editor, /Assign team/);
    const assigner = await renderDetail({permissions: ['incident:view', 'incident:assign']});
    assert.match(assigner, /Assign team/);
    assert.doesNotMatch(assigner, /Edit details|Post comment/);
    assert.doesNotMatch(await renderDetail({permissions: ['incident:update'], status: 'MITIGATED'}), />Resolve</);
    assert.match(await renderDetail({permissions: ['incident:resolve'], status: 'MITIGATED'}), />Resolve</);
    assert.match(await renderDetail({permissions: ['incident:resolve'], status: 'CLOSED'}), /This incident is closed/);
});

test('detail API uses cancellation, history pagination and versioned comment edits', async () => {
    const original = apiClient.defaults.adapter;
    const calls = [];
    const signal = new AbortController().signal;
    try {
        apiClient.defaults.adapter = async config => {
            calls.push(config);
            return {config, headers: {}, status: 200, statusText: 'OK', data: incident};
        };
        await api.getIncident(incident.id, signal);
        await api.getIncidentTimeline(incident.id, 2, signal);
        await api.getIncidentComments(incident.id, 3, signal);
        await api.changeIncident(incident.id, {kind: 'details', title: 'New title', description: 'Details'});
        await api.changeIncident(incident.id, {kind: 'assign', teamId: null, userId: 'user-1'});
        await api.changeIncident(incident.id, {kind: 'severity', severity: 'SEV2'});
        await api.saveIncidentComment(incident.id, 'Updated notes', {id: 'comment-1', version: 7});
        assert.equal(calls[0].url, '/incidents/incident-1');
        assert.equal(calls[0].signal, signal);
        assert.deepEqual(calls[1].params, {page: 2, size: 20, direction: 'DESC'});
        assert.deepEqual(calls[2].params, {page: 3, size: 20});
        assert.equal(calls[3].method, 'put');
        assert.deepEqual(JSON.parse(calls[3].data), {title: 'New title', description: 'Details'});
        assert.equal(calls[4].url, '/incidents/incident-1/assign');
        assert.deepEqual(JSON.parse(calls[4].data), {teamId: null, userId: 'user-1'});
        assert.equal(calls[5].url, '/incidents/incident-1/severity');
        assert.equal(calls[6].method, 'put');
        assert.equal(calls[6].url, '/incidents/incident-1/comments/comment-1');
        assert.deepEqual(JSON.parse(calls[6].data), {comment: 'Updated notes', version: 7});
    } finally { apiClient.defaults.adapter = original; }
});

test('assignment saves preserve the latest untouched assignee and allow explicit unassignment', async () => {
    const original = apiClient.defaults.adapter;
    const writes = [];
    try {
        apiClient.defaults.adapter = async config => {
            if (config.method === 'post') writes.push(JSON.parse(config.data));
            return {config, headers: {}, status: 200, statusText: 'OK', data: {...incident, assignedTeamId: 'latest-team', assignedUserId: 'latest-user'}};
        };
        await api.assignIncident(incident.id, {teamId: 'selected-team'});
        await api.assignIncident(incident.id, {userId: null});
        await api.assignIncident(incident.id, {teamId: null, userId: null});
        assert.deepEqual(writes, [
            {teamId: 'selected-team', userId: 'latest-user'},
            {teamId: 'latest-team', userId: null},
            {teamId: null, userId: null},
        ]);
    } finally {apiClient.defaults.adapter = original;}
});

test('assignment does not write when refreshing the latest incident fails', async () => {
    const original = apiClient.defaults.adapter;
    const calls = [];
    try {
        apiClient.defaults.adapter = async config => {calls.push(config.method); throw new Error('offline');};
        await assert.rejects(api.assignIncident(incident.id, {teamId: 'selected-team'}));
        assert.deepEqual(calls, ['get']);
    } finally {apiClient.defaults.adapter = original;}
});
