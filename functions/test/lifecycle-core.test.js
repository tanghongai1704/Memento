const test = require("node:test");
const assert = require("node:assert/strict");
const {
  parseConnectionMutationInput,
  parsePostMutationInput,
} = require("../lib/lifecycleCore");

test("accepts scoped post and connection mutation IDs", () => {
  assert.deepEqual(parsePostMutationInput({connectionId: "connection-1", postId: "post-1"}), {
    connectionId: "connection-1", postId: "post-1",
  });
  assert.deepEqual(parseConnectionMutationInput({connectionId: "connection-1"}), {
    connectionId: "connection-1",
  });
});

test("rejects missing, path-like and oversized mutation IDs", () => {
  assert.equal(parsePostMutationInput({connectionId: "connection/1", postId: "post-1"}), null);
  assert.equal(parsePostMutationInput({connectionId: "connection-1"}), null);
  assert.equal(parseConnectionMutationInput({connectionId: "x".repeat(129)}), null);
});
