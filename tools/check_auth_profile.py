"""Two-account Auth/Profile smoke test; only localhost emulators are permitted.
Does not run Android code. Never accepts a real Firebase project or API key.
"""
import json
import os
import time
import urllib.error
import urllib.request

PROJECT = 'demo-memento-schema'

def local_host(key):
    host = os.environ.get(key, '')
    if not host or host.split(':')[0] not in ('127.0.0.1', 'localhost'):
        raise RuntimeError(f'{key} must point to a localhost emulator')
    return host

AUTH = 'http://' + local_host('FIREBASE_AUTH_EMULATOR_HOST') + '/identitytoolkit.googleapis.com/v1'
FIRESTORE = 'http://' + local_host('FIRESTORE_EMULATOR_HOST') + '/v1'
DOCUMENTS = f'projects/{PROJECT}/databases/(default)/documents'
checks = 0

def request(url, body=None, token=None, expected=200):
    global checks
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(url, headers=headers,
        data=json.dumps(body).encode() if body is not None else None)
    try:
        with urllib.request.urlopen(req, timeout=20) as response:
            status, content = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, content = error.code, error.read()
    if status != expected:
        raise AssertionError(f'Expected {expected}, got {status}: {content.decode()}')
    checks += 1
    return json.loads(content) if content else None


def create_profile(uid, token, label):
    return request(FIRESTORE + '/' + DOCUMENTS + ':commit', {'writes': [{
        'update': {'name': DOCUMENTS + '/users/' + uid, 'fields': {
            'displayName': {'stringValue': label}, 'username': {'stringValue': label},
            'usernameNormalized': {'stringValue': label.lower()},
            'avatarPath': {'nullValue': None}, 'bio': {'nullValue': None},
            'schemaVersion': {'integerValue': '1'}}},
        'updateTransforms': [{'fieldPath': field, 'setToServerValue': 'REQUEST_TIME'}
            for field in ['createdAt', 'updatedAt']]}]}, token)

suffix = str(time.time_ns())
accounts = []
emails = []
for label in ['Alice', 'Bob']:
    credentials = {'email': f'{label.lower()}-{suffix}@example.test',
        'password': 'EmulatorOnly-123!', 'returnSecureToken': True}
    registered = request(AUTH + '/accounts:signUp?key=emulator-only', credentials)
    create_profile(registered['localId'], registered['idToken'], label)
    before = request(FIRESTORE + '/' + DOCUMENTS + '/users/' + registered['localId'], token=registered['idToken'])
    assert set(before['fields']) == {'displayName', 'username', 'usernameNormalized', 'avatarPath', 'bio', 'createdAt', 'updatedAt', 'schemaVersion'}
    signed_in = request(AUTH + '/accounts:signInWithPassword?key=emulator-only', credentials)
    assert signed_in['localId'] == registered['localId']
    after = request(FIRESTORE + '/' + DOCUMENTS + '/users/' + signed_in['localId'], token=signed_in['idToken'])
    assert before['fields'] == after['fields'], 'Login must not reset a profile'
    accounts.append(signed_in)
    emails.append(credentials['email'])

reset = request(AUTH + '/accounts:sendOobCode?key=emulator-only', {
    'requestType': 'PASSWORD_RESET',
    'email': emails[0]
})
assert reset['email'] == emails[0]

request(FIRESTORE + '/' + DOCUMENTS + ':commit', {'writes': [{
    'update': {'name': DOCUMENTS + '/users/' + accounts[0]['localId'],
        'fields': {'displayName': {'stringValue': 'Not allowed'}}},
    'updateMask': {'fieldPaths': ['displayName']}}]}, accounts[1]['idToken'], expected=403)
request(FIRESTORE + '/' + DOCUMENTS + '/users/' + accounts[0]['localId'], expected=403)
print(f'Auth/Profile: two accounts registered and signed in, password reset requested; {checks} checks passed (emulators only).')
