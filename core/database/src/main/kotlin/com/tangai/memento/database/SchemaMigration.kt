package com.tangai.memento.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE users_new (id TEXT NOT NULL PRIMARY KEY, username TEXT NOT NULL, displayName TEXT NOT NULL, usernameNormalized TEXT NOT NULL, avatarPath TEXT, bio TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, schemaVersion INTEGER NOT NULL)")
        db.execSQL("INSERT INTO users_new SELECT id, username, username, lower(replace(username, ' ', '')), NULL, NULL, createdAt, createdAt, 1 FROM users")
        db.execSQL("DROP TABLE users")
        db.execSQL("ALTER TABLE users_new RENAME TO users")
        db.execSQL("CREATE INDEX index_users_usernameNormalized ON users(usernameNormalized)")
        // Rebuild parent and child together; legacy remote connections must be migrated separately.
        db.execSQL("CREATE TABLE connections_new (id TEXT NOT NULL PRIMARY KEY, type TEXT NOT NULL, status TEXT NOT NULL, name TEXT, ownerId TEXT, maxMembers INTEGER NOT NULL, directKey TEXT, lastPostAt INTEGER, schemaVersion INTEGER NOT NULL, createdBy TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
        db.execSQL("INSERT INTO connections_new SELECT id, type, 'CLOSED', name, NULL, 2, NULL, NULL, 1, createdBy, createdAt, updatedAt FROM connections")
        db.execSQL("CREATE TABLE members_backup AS SELECT * FROM connection_members")
        db.execSQL("DROP TABLE connection_members")
        db.execSQL("DROP TABLE connections")
        db.execSQL("ALTER TABLE connections_new RENAME TO connections")
        db.execSQL("CREATE INDEX index_connections_status ON connections(status)")
        db.execSQL("CREATE TABLE connection_members (connectionId TEXT NOT NULL, userId TEXT NOT NULL, role TEXT NOT NULL, joinedAt INTEGER NOT NULL, status TEXT NOT NULL, leftAt INTEGER, invitedBy TEXT, removedBy TEXT, PRIMARY KEY(connectionId, userId), FOREIGN KEY(connectionId) REFERENCES connections(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("INSERT INTO connection_members SELECT connectionId, userId, upper(role), joinedAt, status, NULL, NULL, NULL FROM members_backup")
        db.execSQL("DROP TABLE members_backup")
        db.execSQL("CREATE INDEX index_connection_members_connectionId ON connection_members(connectionId)")
        db.execSQL("CREATE INDEX index_connection_members_userId ON connection_members(userId)")
        db.execSQL("DROP TABLE IF EXISTS connection_requests")
        db.execSQL("CREATE TABLE posts (id TEXT NOT NULL, connectionId TEXT NOT NULL, authorId TEXT NOT NULL, postType TEXT NOT NULL, layoutType TEXT NOT NULL, caption TEXT, clientCreatedAt INTEGER NOT NULL, createdAt INTEGER, updatedAt INTEGER, status TEXT NOT NULL, deletedAt INTEGER, deletedBy TEXT, schemaVersion INTEGER NOT NULL, localSyncStatus TEXT NOT NULL, PRIMARY KEY(connectionId, id))")
        db.execSQL("CREATE INDEX index_posts_connectionId ON posts(connectionId)")
        db.execSQL("CREATE INDEX index_posts_authorId ON posts(authorId)")
        db.execSQL("CREATE INDEX index_posts_createdAt ON posts(createdAt)")
        db.execSQL("CREATE INDEX index_posts_status_createdAt ON posts(status, createdAt)")
        db.execSQL("CREATE INDEX index_posts_connectionId_status_createdAt ON posts(connectionId, status, createdAt)")
        db.execSQL("CREATE TABLE media_items (connectionId TEXT NOT NULL, postId TEXT NOT NULL, mediaId TEXT NOT NULL, mediaType TEXT NOT NULL, storagePath TEXT NOT NULL, thumbnailPath TEXT, mimeType TEXT NOT NULL, width INTEGER NOT NULL, height INTEGER NOT NULL, durationMs INTEGER, sizeBytes INTEGER NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(connectionId, postId, mediaId), FOREIGN KEY(connectionId, postId) REFERENCES posts(connectionId, id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX index_media_items_postId ON media_items(postId)")
        db.execSQL("CREATE INDEX index_media_items_connectionId_postId ON media_items(connectionId, postId)")
    }
}
