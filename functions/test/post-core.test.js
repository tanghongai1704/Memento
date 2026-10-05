const test = require("node:test");
const assert = require("node:assert/strict");
const {parseFinalizePhotoInput, MAX_PHOTO_BYTES} = require("../lib/postCore");

const valid = {
  connectionId: "connection-1", postId: "post-1", clientCreatedAt: 1000,
  caption: "hello", layoutType: "SINGLE", mediaItems: [{
    mediaId: "media-1", storagePath: "connections/connection-1/posts/post-1/media-1.jpg",
    mimeType: "image/jpeg", width: 1920, height: 1080, sizeBytes: 1234, position: 0,
  }],
};

test("accepts the exact MVP photo contract", () => {
  assert.deepEqual(parseFinalizePhotoInput(valid), valid);
});

test("keeps accepting the legacy single-photo client payload", () => {
  const media = valid.mediaItems[0];
  const legacy = {
    connectionId: valid.connectionId,
    postId: valid.postId,
    clientCreatedAt: valid.clientCreatedAt,
    caption: valid.caption,
    mediaId: media.mediaId,
    storagePath: media.storagePath,
    mimeType: media.mimeType,
    width: media.width,
    height: media.height,
    sizeBytes: media.sizeBytes,
  };
  assert.deepEqual(parseFinalizePhotoInput(legacy), valid);
});

test("rejects paths, media shape and size outside the contract", () => {
  assert.equal(parseFinalizePhotoInput({...valid, mediaItems: [{...valid.mediaItems[0], storagePath: "other.jpg"}]}), null);
  assert.equal(parseFinalizePhotoInput({...valid, mediaItems: [{...valid.mediaItems[0], mimeType: "image/png"}]}), null);
  assert.equal(parseFinalizePhotoInput({...valid, mediaItems: [{...valid.mediaItems[0], width: 1921}]}), null);
  assert.equal(parseFinalizePhotoInput({...valid, mediaItems: [{...valid.mediaItems[0], sizeBytes: MAX_PHOTO_BYTES + 1}]}), null);
  assert.equal(parseFinalizePhotoInput({...valid, caption: "x".repeat(1001)}), null);
});

test("accepts ordered multi-photo layouts and rejects invalid shape", () => {
  const second = {...valid.mediaItems[0], mediaId: "media-2", position: 1,
    storagePath: "connections/connection-1/posts/post-1/media-2.jpg"};
  const multi = {...valid, layoutType: "GRID", mediaItems: [valid.mediaItems[0], second]};
  assert.deepEqual(parseFinalizePhotoInput(multi), multi);
  assert.equal(parseFinalizePhotoInput({...multi, layoutType: "SINGLE"}), null);
  assert.equal(parseFinalizePhotoInput({...multi, mediaItems: [second, valid.mediaItems[0]]}), null);
  assert.equal(parseFinalizePhotoInput({...multi, mediaItems: Array(6).fill(valid.mediaItems[0])}), null);
});
