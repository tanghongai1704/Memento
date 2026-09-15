"""Rules integration checks against a local demo Firestore emulator only."""
import base64
import json
import os
import time
import urllib.error
import urllib.request

host = os.environ.get('FIRESTORE_EMULATOR_HOST')
assert host and host.split(':')[0] in ('127.0.0.1', 'localhost'), 'Local emulator required'
project = 'demo-memento-schema'
root = f'http://{host}/v1/projects/{project}/databases/(default)/documents'
name = f'projects/{project}/databases/(default)/documents'


def token(uid):
    def enc(data):
        return base64.urlsafe_b64encode(json.dumps(data).encode()).decode().rstrip('=')
    return enc({'alg': 'none', 'typ': 'JWT'}) + '.' + enc({
        'sub': uid, 'user_id': uid, 'aud': project, 'iss': f'https://securetoken.google.com/{project}',
        'iat': int(time.time()), 'exp': int(time.time()) + 3600,
        'firebase': {'sign_in_provider': 'password'}}) + '.'


def call(path, uid=None, body=None, method=None, expected=200):
    headers = {'Content-Type': 'application/json'}
    if uid:
        headers['Authorization'] = 'Bearer ' + ('owner' if uid == 'ADMIN' else token(uid))
    req = urllib.request.Request(root + path, data=json.dumps(body).encode() if body is not None else None,
                                 headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as response:
            status, data = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, data = error.code, error.read()
    assert status == expected, (path, status, expected, data.decode())
    return json.loads(data) if data else None


def profile(uid, display='Alice'):
    return {'update': {'name': name + '/users/' + uid, 'fields': {
        'displayName': {'stringValue': display}, 'username': {'stringValue': 'Alice'},
        'usernameNormalized': {'stringValue': 'alice'}, 'avatarPath': {'nullValue': None},
        'bio': {'nullValue': None}, 'schemaVersion': {'integerValue': '1'}}},
        'updateTransforms': [{'fieldPath': field, 'setToServerValue': 'REQUEST_TIME'} for field in ['createdAt', 'updatedAt']]}


call(':commit', 'a', {'writes': [profile('a')]})
original = call('/users/a', 'a')
update = profile('a', 'Updated name')
update['update']['fields']['username'] = {'stringValue': 'Alice.2'}
update['update']['fields']['usernameNormalized'] = {'stringValue': 'alice.2'}
update['update']['fields']['createdAt'] = original['fields']['createdAt']
update['updateTransforms'] = [t for t in update['updateTransforms'] if t['fieldPath'] == 'updatedAt']
call(':commit', 'a', {'writes': [update]})
invalid_normalization = profile('a')
invalid_normalization['update']['fields']['usernameNormalized'] = {'stringValue': 'not-alice'}
call(':commit', 'a', {'writes': [invalid_normalization]}, expected=403)
call('/users/a', expected=403)
call(':commit', 'b', {'writes': [profile('a', 'Hacked')]}, expected=403)
call(':commit', 'a', {'writes': [profile('a', 'x' * 501)]}, expected=403)
call(':runQuery', 'a', {'structuredQuery': {'from': [{'collectionId': 'users'}]}}, expected=403)
call(':runQuery', 'a', {'structuredQuery': {'from': [{'collectionId': 'users'}], 'limit': 20,
    'where': {'fieldFilter': {'field': {'fieldPath': 'usernameNormalized'}, 'op': 'EQUAL', 'value': {'stringValue': 'alice'}}}}})
call(':runQuery', 'a', {'structuredQuery': {'from': [{'collectionId': 'invites'}], 'limit': 20}}, expected=403)
call('/invites/known-hash', 'a', expected=403)
for internal_collection in ['directInviteOwners', 'directConnectionLocks', 'inviteRedeemRateLimits',
                            'userInviteCodes', 'inviteCodeLookup']:
    call('/' + internal_collection + '/test', 'a', expected=403)
call(':commit', 'a', {'writes': [{'update': {
    'name': name + '/directInviteOwners/a', 'fields': {'codeHash': {'stringValue': 'forged'}}}}]}, expected=403)
call(':commit', 'a', {'writes': [{'update': {'name': name + '/connections/c', 'fields': {}}}]}, expected=403)
# Admin seed is scoped to the local demo emulator.
call(':commit', 'ADMIN', {'writes': [
    {'update': {'name': name + '/connections/c', 'fields': {'status': {'stringValue': 'ACTIVE'},
        'memberIds': {'arrayValue': {'values': [{'stringValue': 'a'}]}}, 'lastPostAt': {'nullValue': None}}}},
    {'update': {'name': name + '/connections/c/members/a', 'fields': {'status': {'stringValue': 'ACTIVE'}}}}
]})
call('/connections/c', 'a')
call('/connections/c/members/a', 'a')
call('/connections/c', 'b', expected=403)
call('/connections/c/members/a', 'b', expected=403)
call(':runQuery', 'a', {'structuredQuery': {'from': [{'collectionId': 'connections'}]}}, expected=403)
call(':runQuery', 'a', {'structuredQuery': {'from': [{'collectionId': 'connections'}],
    'where': {'compositeFilter': {'op': 'AND', 'filters': [
        {'fieldFilter': {'field': {'fieldPath': 'memberIds'}, 'op': 'ARRAY_CONTAINS', 'value': {'stringValue': 'a'}}},
        {'fieldFilter': {'field': {'fieldPath': 'status'}, 'op': 'EQUAL', 'value': {'stringValue': 'ACTIVE'}}}]}},
    'orderBy': [{'field': {'fieldPath': 'lastPostAt'}, 'direction': 'DESCENDING'}]}})
print('Firestore rules: 22 access/validation checks passed (local demo emulator).')
