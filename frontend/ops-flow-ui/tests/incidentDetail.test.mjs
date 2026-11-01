import assert from 'node:assert/strict';
import {test} from 'node:test';
import {MutationObserver, QueryClient} from '@tanstack/react-query';
import {incidentMutationOptions} from '@/modules/incidents/query/incidentMutationOptions.ts';
import {incidentKeys} from '@/modules/incidents/query/incidentKeys.ts';
import {incidentCapabilities} from '@/modules/incidents/utils/incidentDetail.ts';

const incident = {id: 'incident-1', version: 1};
function deferred() {
    let resolve;
    const promise = new Promise(done => {resolve = done;});
    return {promise, resolve};
}

test('incident writes from separate controls serialize and retain independent pending state', async () => {
    const client = new QueryClient();
    const firstStarted = deferred();
    const releaseFirst = deferred();
    const order = [];
    const first = new MutationObserver(client, incidentMutationOptions(client, incident.id, async () => {
        order.push('severity'); firstStarted.resolve(); await releaseFirst.promise;
        return {...incident, version: 2};
    }));
    const second = new MutationObserver(client, incidentMutationOptions(client, incident.id, async () => {
        order.push('assignment');
        assert.equal(client.getQueryData(incidentKeys.detail(incident.id)).version, 2);
        return {...incident, version: 3};
    }));
    try {
        const severity = first.mutate();
        await firstStarted.promise;
        assert.equal(first.getCurrentResult().isPending, true);
        assert.equal(second.getCurrentResult().isPending, false);
        const assignment = second.mutate();
        assert.equal(second.getCurrentResult().isPaused, true);
        assert.deepEqual(order, ['severity']);
        releaseFirst.resolve();
        await Promise.all([severity, assignment]);
        assert.deepEqual(order, ['severity', 'assignment']);
        assert.equal(client.getQueryData(incidentKeys.detail(incident.id)).version, 3);
    } finally {releaseFirst.resolve(); client.clear();}
});

test('an older mutation response cannot replace a newer incident cache entry', async () => {
    const client = new QueryClient();
    client.setQueryData(incidentKeys.detail(incident.id), {...incident, version: 5});
    const mutation = new MutationObserver(client, incidentMutationOptions(client, incident.id, async () => ({...incident, version: 4})));
    try {
        await mutation.mutate();
        assert.equal(client.getQueryData(incidentKeys.detail(incident.id)).version, 5);
    } finally {client.clear();}
});

test('failed writes release the incident queue for subsequent controls', async () => {
    const client = new QueryClient();
    const failure = new MutationObserver(client, incidentMutationOptions(client, incident.id, async () => {throw new Error('conflict');}));
    const success = new MutationObserver(client, incidentMutationOptions(client, incident.id, async () => incident));
    try {
        const failed = failure.mutate();
        const succeeded = success.mutate();
        await assert.rejects(failed, /conflict/);
        await succeeded;
        assert.equal(success.getCurrentResult().isSuccess, true);
    } finally {client.clear();}
});

test('capabilities enforce workflow authority and comment authorship, including administrators', () => {
    const viewer = {id: 'viewer', roles: [], permissions: ['incident:view']};
    const author = {id: 'author', roles: [], permissions: ['incident:update']};
    const admin = {id: 'admin', roles: ['ROLE_PLATFORM_ADMIN'], permissions: []};
    assert.equal(incidentCapabilities(viewer, 'OPEN').workflow, undefined);
    assert.equal(incidentCapabilities(author, 'OPEN').workflow.action, 'acknowledge');
    assert.equal(incidentCapabilities(author, 'MITIGATED').workflow, undefined);
    assert.equal(incidentCapabilities(admin, 'MITIGATED').workflow.action, 'resolve');
    assert.equal(incidentCapabilities(admin, 'CLOSED').workflow, undefined);
    assert.equal(incidentCapabilities(author, 'OPEN').canEditComment({userId: 'author'}), true);
    assert.equal(incidentCapabilities(admin, 'OPEN').canEditComment({userId: 'author'}), false);
    assert.equal(incidentCapabilities(null, 'OPEN').canAssign, false);
});
