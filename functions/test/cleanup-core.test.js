const test = require("node:test");
const assert = require("node:assert/strict");
const {
  MEDIA_CLEANUP_GRACE_MS,
  parseStoredMediaPath,
  shouldDeleteStoredMedia,
} = require("../lib/cleanupCore");

const path = "connections/connection-1/posts/post-1/media-1.jpg";
const nowMillis = Date.UTC(2026, 8, 17);

test("accepts only the exact managed photo path", () => {
  assert.deepEqual(parseStoredMediaPath(path), {
    connectionId: "connection-1", postId: "post-1", mediaId: "media-1",
  });
  assert.equal(parseStoredMediaPath(path + ".tmp"), null);
  assert.equal(parseStoredMediaPath("avatars/user/photo.jpg"), null);
  assert.equal(parseStoredMediaPath("connections/a/posts/b/../c.jpg"), null);
});

test("keeps recent and active media", () => {
  assert.equal(shouldDeleteStoredMedia({
    path, createdAtMillis: nowMillis - MEDIA_CLEANUP_GRACE_MS + 1, nowMillis, postExists: false,
  }), false);
  assert.equal(shouldDeleteStoredMedia({
    path, createdAtMillis: nowMillis - MEDIA_CLEANUP_GRACE_MS, nowMillis,
    postExists: true, postStatus: "ACTIVE", postStoragePaths: [path],
  }), false);
});

test("deletes expired orphan and matching soft-deleted media only", () => {
  const old = nowMillis - MEDIA_CLEANUP_GRACE_MS;
  assert.equal(shouldDeleteStoredMedia({
    path, createdAtMillis: old, nowMillis, postExists: false,
  }), true);
  assert.equal(shouldDeleteStoredMedia({
    path, createdAtMillis: old, nowMillis, postExists: true, postStatus: "DELETED",
    deletedAtMillis: old, postStoragePaths: [path],
  }), true);
  assert.equal(shouldDeleteStoredMedia({
    path, createdAtMillis: old, nowMillis, postExists: true, postStatus: "DELETED",
    deletedAtMillis: old, postStoragePaths: ["connections/other/posts/post/media.jpg"],
  }), false);
});
