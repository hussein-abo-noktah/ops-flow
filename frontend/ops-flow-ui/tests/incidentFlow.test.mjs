import assert from 'node:assert/strict';
import {after, before, test} from 'node:test';
import {createServer} from 'vite';
import {createElement as h} from 'react';
import {renderToStaticMarkup} from 'react-dom/server';
import {Provider} from 'react-redux';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {incidentKeys} from '@/modules/incidents/query/incidentKeys.ts';
import {readIncidentParams} from '@/modules/incidents/utils/incidentList.ts';

let server, IncidentsPage, store, setAuth, apiClient, api;
const incident = {
    id: 'incident-1', incidentNumber: 'INC-001', title: 'Database timeout', description: 'Investigate connection pool',
    status: 'OPEN', severity: 'SEV1', createdBy: 'user-1', createdAt: '2026-10-31T10:00:00Z', updatedAt: '2026-10-31T10:00:00Z', version: 0,
};
const page = {items: [incident], page: 0, size: 20, totalItems: 1, totalPages: 1, first: true, last: true};

before(async () => {
    server = await createServer({server: {middlewareMode: true, hmr: false}, appType: 'custom'});
    ({IncidentsPage} = await server.ssrLoadModule('/src/modules/incidents/pages/IncidentsPage.tsx'));
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
