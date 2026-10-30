import assert from 'node:assert/strict';
import {test} from 'node:test';
import {hasPermission} from '@/modules/auth/utils/permissions.ts';

const member = {
    id: 'user-1', organizationId: 'org-1', roles: [], permissions: ['incident:view'],
};

test('anonymous users cannot access even an unrestricted workspace page', () => {
    assert.equal(hasPermission(null), false);
    assert.equal(hasPermission(null, 'incident:view'), false);
});

test('authenticated members can access pages without a permission requirement', () => {
    assert.equal(hasPermission({...member, permissions: []}), true);
});

test('a member needs the exact resource and action permission', () => {
    assert.equal(hasPermission(member, 'incident:view'), true);
    assert.equal(hasPermission(member, 'incident:update'), false);
    assert.equal(hasPermission(member, 'user:view'), false);
    assert.equal(hasPermission({...member, permissions: ['incident:*']}, 'incident:view'), false);
});

test('platform administrators can access modules without explicit permissions', () => {
    const admin = {...member, roles: ['ROLE_PLATFORM_ADMIN'], permissions: []};
    for (const permission of ['incident:view', 'user:view', 'audit:view']) {
        assert.equal(hasPermission(admin, permission), true);
    }
});

test('other roles do not implicitly bypass permission checks', () => {
    assert.equal(hasPermission({...member, roles: ['ROLE_ORG_ADMIN']}, 'audit:view'), false);
});

test('removing a permission or administrator role removes access', () => {
    assert.equal(hasPermission({...member, permissions: []}, 'incident:view'), false);
    assert.equal(hasPermission({...member, roles: [], permissions: []}, 'audit:view'), false);
});
