const test = require("node:test");
const assert = require("node:assert/strict");
const {
  DIRECT_CODE_ALPHABET,
  directKeyFor,
  formatInviteCode,
  generateInviteCode,
  normalizeInviteCode,
  sha256,
} = require("../lib/inviteCore");

test("normalizes and formats direct invite codes", () => {
  assert.equal(normalizeInviteCode(" abcd-2345 "), "ABCD2345");
  assert.equal(formatInviteCode("ABCD2345"), "ABCD-2345");
  assert.equal(normalizeInviteCode("ABCI-2345"), null);
  assert.equal(normalizeInviteCode("short"), null);
});

test("generates codes from the unambiguous alphabet", () => {
  for (let index = 0; index < 100; index += 1) {
    const code = generateInviteCode();
    assert.equal(code.length, 8);
    assert.ok([...code].every((character) => DIRECT_CODE_ALPHABET.includes(character)));
  }
});

test("hash and direct key are stable", () => {
  assert.equal(sha256("ABC"), "b5d4045c3f466fa91fe2cc6abe79232a1a57cdf104f7a26e716e0a1e2789df78");
  assert.equal(directKeyFor("uid-b", "uid-a"), directKeyFor("uid-a", "uid-b"));
});
