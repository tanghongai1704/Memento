const test = require("node:test");
const assert = require("node:assert/strict");
const {MEDIA_CLEANUP_GRACE_MS} = require("../lib/cleanupCore");
const {cleanupExpiredMedia} = require("../lib/cleanupService");

const timestamp = (millis) => ({toMillis: () => millis});

test("cleanup deletes only expired orphan and matching deleted media", async () => {
  const now = Date.UTC(2026, 8, 17);
  const old = new Date(now - MEDIA_CLEANUP_GRACE_MS).toISOString();
  const recent = new Date(now - MEDIA_CLEANUP_GRACE_MS + 1).toISOString();
  const paths = {
    orphan: "connections/c/posts/orphan/media.jpg",
    deleted: "connections/c/posts/deleted/media.jpg",
    active: "connections/c/posts/active/media.jpg",
    recent: "connections/c/posts/recent/media.jpg",
    mismatch: "connections/c/posts/mismatch/media.jpg",
  };
  const removed = [];
  const files = [
    ...Object.entries(paths).map(([key, name]) => ({
      name,
      metadata: {timeCreated: key === "recent" ? recent : old},
      delete: async () => removed.push(name),
    })),
    {name: "avatars/user/photo.jpg", metadata: {timeCreated: old}, delete: async () => {
      throw new Error("must not delete unmanaged paths");
    }},
  ];
  const posts = {
    orphan: {exists: false},
    deleted: {exists: true, status: "DELETED", deletedAt: timestamp(now - MEDIA_CLEANUP_GRACE_MS),
      mediaItems: [{storagePath: paths.deleted}]},
    active: {exists: true, status: "ACTIVE", mediaItems: [{storagePath: paths.active}]},
    recent: {exists: false},
    mismatch: {exists: true, status: "DELETED", deletedAt: timestamp(now - MEDIA_CLEANUP_GRACE_MS),
      mediaItems: [{storagePath: "connections/c/posts/mismatch/other.jpg"}]},
  };
  const db = {collection: () => ({doc: () => ({collection: () => ({doc: (postId) => ({
    get: async () => ({
      exists: posts[postId].exists,
      get: (field) => posts[postId][field],
    }),
  })})})})};
  const bucket = {getFiles: async () => [files]};

  const result = await cleanupExpiredMedia(db, bucket, now);

  assert.deepEqual(removed.sort(), [paths.deleted, paths.orphan].sort());
  assert.deepEqual(result, {scanned: 6, deleted: 2, skipped: 4});
});
