import assert from 'node:assert/strict';
import {test} from 'node:test';
import {AxiosError} from 'axios';
import {normalizeApiError} from '@/shared/api/normalizeApiError.ts';
import {ApiError} from '@/shared/api/apiError.ts';

function httpError(status, data) {
    return new AxiosError('Request failed', 'ERR_BAD_RESPONSE', undefined, undefined, {
        status, data, statusText: '', headers: {}, config: {},
    });
}

test('empty unauthorized responses retain HTTP status and explain authentication failure', () => {
    for (const body of ['', undefined, null]) {
        const result = normalizeApiError(httpError(401, body));
        assert.equal(result.status, 401);
        assert.equal(result.type, 'HTTP_ERROR');
        assert.equal(result.message, 'Invalid credentials or token.');
    }
});

test('non-JSON server or proxy failures do not appear as connectivity failures', () => {
    const result = normalizeApiError(httpError(502, '<html>Bad gateway</html>'));
    assert.equal(result.status, 502);
    assert.equal(result.type, 'HTTP_ERROR');
    assert.match(result.message, /server could not complete/);
});

test('Problem Details and validation errors remain available', () => {
    const result = normalizeApiError(httpError(400, {
        type: 'validation', detail: 'Invalid email', validationErrors: {email: 'Required'},
    }));
    assert.equal(result.status, 400);
    assert.equal(result.message, 'Invalid email');
    assert.deepEqual(result.validationErrors, {email: 'Required'});
});

test('only requests without HTTP responses report connectivity or timeout errors', () => {
    assert.equal(normalizeApiError(new AxiosError('Network Error', 'ERR_NETWORK')).type, 'NETWORK_ERROR');
    for (const code of ['ECONNABORTED', 'ETIMEDOUT']) {
        assert.equal(normalizeApiError(new AxiosError('timeout', code)).type, 'REQUEST_TIMEOUT');
    }
});

test('already normalized API errors are preserved', () => {
    const original = new ApiError(401, 'NO_SESSION', 'Please sign in');
    assert.equal(normalizeApiError(original), original);
});
