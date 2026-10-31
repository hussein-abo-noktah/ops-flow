import assert from 'node:assert/strict';
import {test} from 'node:test';
import {QueryClient} from '@tanstack/react-query';
import {readIncidentParams, statusActions} from '@/modules/incidents/utils/incidentList.ts';
import {incidentKeys} from '@/modules/incidents/query/incidentKeys.ts';
import {hasPermission} from '@/modules/auth/utils/permissions.ts';

test('empty URL produces backend-compatible pagination and sorting defaults', () => {
    assert.deepEqual(readIncidentParams(new URLSearchParams()), {
        page: 0, size: 20, q: undefined, status: undefined, severity: undefined,
        sortBy: 'createdAt', direction: 'DESC',
    });
});

test('bookmarked search, filters, page size and sorting survive a URL round trip', () => {
    const url = new URL('https://opsflow.test/incidents?page=3&size=50&q=database+timeout&status=INVESTIGATING&severity=SEV1&sortBy=updatedAt&direction=ASC');
    const params = readIncidentParams(url.searchParams);
    assert.deepEqual(params, {
        page: 3, size: 50, q: 'database timeout', status: 'INVESTIGATING', severity: 'SEV1',
        sortBy: 'updatedAt', direction: 'ASC',
    });
    assert.deepEqual(readIncidentParams(new URLSearchParams(url.searchParams.toString())), params);
});

test('invalid URL values never reach backend validation', () => {
    for (const page of ['-1', '1.5', 'NaN', 'Infinity', '1e4', '9999999999999999999', '2147483647']) {
        const params = readIncidentParams(new URLSearchParams({page, size: '101', status: 'INVALID', severity: 'urgent', sortBy: 'password', direction: 'wrong'}));
        assert.equal(params.page, 0);
        assert.equal(params.size, 20);
        assert.equal(params.status, undefined);
        assert.equal(params.severity, undefined);
        assert.equal(params.sortBy, 'createdAt');
        assert.equal(params.direction, 'DESC');
    }
    assert.equal(readIncidentParams(new URLSearchParams('size=0')).size, 20);
    assert.equal(readIncidentParams(new URLSearchParams('size=100&page=21474836')).page, 21474836);
    assert.equal(readIncidentParams(new URLSearchParams('size=100&page=21474837')).page, 0);
});

test('search trims whitespace, limits length and supports existing search URLs', () => {
    assert.equal(readIncidentParams(new URLSearchParams('search=++timeout++')).q, 'timeout');
    assert.equal(readIncidentParams(new URLSearchParams('search=old&q=new')).q, 'new');
    assert.equal(readIncidentParams(new URLSearchParams('q=+++')).q, undefined);
    assert.equal(readIncidentParams(new URLSearchParams({q: 'x'.repeat(300)})).q.length, 250);
});

test('query keys isolate each list and list invalidation preserves detail caches', async () => {
    const client = new QueryClient();
    const first = incidentKeys.list(readIncidentParams(new URLSearchParams()));
    const second = incidentKeys.list(readIncidentParams(new URLSearchParams('page=1')));
    const filtered = incidentKeys.list(readIncidentParams(new URLSearchParams('status=OPEN')));
    const detail = incidentKeys.detail('incident-1');
    for (const [index, key] of [first, second, filtered, detail].entries()) client.setQueryData(key, {index});
    assert.equal(client.getQueryData(first).index, 0);
    assert.equal(client.getQueryData(second).index, 1);
    assert.equal(client.getQueryData(filtered).index, 2);
    await client.invalidateQueries({queryKey: incidentKeys.lists()});
    for (const key of [first, second, filtered]) assert.equal(client.getQueryState(key).isInvalidated, true);
    assert.equal(client.getQueryState(detail).isInvalidated, false);
    client.clear();
});

test('lifecycle actions follow backend transitions and require the matching authority', () => {
    const editor = {roles: [], permissions: ['incident:update']};
    const resolver = {roles: [], permissions: ['incident:resolve']};
    const viewer = {roles: [], permissions: ['incident:view']};
    const admin = {roles: ['ROLE_PLATFORM_ADMIN'], permissions: []};
    const actions = ['acknowledge', 'start-investigation', 'mitigate', 'resolve', 'close'];
    for (const [index, status] of ['OPEN', 'ACKNOWLEDGED', 'INVESTIGATING', 'MITIGATED', 'RESOLVED'].entries()) {
        const next = statusActions[status];
        assert.equal(next.action, actions[index]);
        assert.equal(hasPermission(editor, next.permission), index < 3);
        assert.equal(hasPermission(resolver, next.permission), index >= 3);
        assert.equal(hasPermission(viewer, next.permission), false);
        assert.equal(hasPermission(admin, next.permission), true);
    }
    assert.equal(statusActions.CLOSED, undefined);
});
