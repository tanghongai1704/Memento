const test = require("node:test");
const assert = require("node:assert/strict");
const {parseFinalizePhotoInput, MAX_PHOTO_BYTES} = require("../lib/postCore");

const valid = {
  connectionId: "connection-1", postId: "post-1", mediaId: "media-1", clientCreatedAt: 1000,
  caption: "hello", storagePath: "connections/connection-1/posts/post-1/media-1.jpg",
  mimeType: "image/jpeg", width: 1920, height: 1080, sizeBytes: 1234,
};

test("accepts the exact MVP photo contract", () => {
  assert.deepEqual(parseFinalizePhotoInput(valid), valid);
});

test("rejects paths, media shape and size outside the contract", () => {
  assert.equal(parseFinalizePhotoInput({...valid, storagePath: "other.jpg"}), null);
  assert.equal(parseFinalizePhotoInput({...valid, mimeType: "image/png"}), null);
  assert.equal(parseFinalizePhotoInput({...valid, width: 1921}), null);
  assert.equal(parseFinalizePhotoInput({...valid, sizeBytes: MAX_PHOTO_BYTES + 1}), null);
  assert.equal(parseFinalizePhotoInput({...valid, caption: "x".repeat(1001)}), null);
});
