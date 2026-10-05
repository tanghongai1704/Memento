"""Exercise actual migration and DAO SQL on SQLite; no Android device required.
Run assembleDebug first to also compare the migrated schema against Room's generated DDL.
"""
from pathlib import Path
import re
import sqlite3
import unittest

ROOT = Path(__file__).resolve().parents[1]
DB = ROOT / 'core/database'
SRC = DB / 'src/main/kotlin/com/tangai/memento/database'


def migrate():
    db = sqlite3.connect(':memory:')
    db.execute('PRAGMA foreign_keys = ON')
    db.executescript('''
        CREATE TABLE users (id TEXT NOT NULL PRIMARY KEY, username TEXT NOT NULL, email TEXT NOT NULL, createdAt INTEGER NOT NULL);
        CREATE TABLE connections (id TEXT NOT NULL PRIMARY KEY, type TEXT NOT NULL, status TEXT NOT NULL, name TEXT, description TEXT, createdBy TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL);
        CREATE TABLE connection_members (connectionId TEXT NOT NULL, userId TEXT NOT NULL, role TEXT NOT NULL, joinedAt INTEGER NOT NULL, status TEXT NOT NULL, PRIMARY KEY(connectionId,userId), FOREIGN KEY(connectionId) REFERENCES connections(id) ON DELETE CASCADE);
        CREATE TABLE connection_requests (id TEXT PRIMARY KEY);
        INSERT INTO users VALUES ('a','Alice','private@example.com',100);
        INSERT INTO connections VALUES ('c','DIRECT','ACTIVE',NULL,NULL,'a',100,100);
        INSERT INTO connection_members VALUES ('c','a','member',100,'ACTIVE');
    ''')
    for sql in re.findall(r'db.execSQL\("(.*?)"\)', (SRC / 'SchemaMigration.kt').read_text()):
        db.execute(sql)
    return db


class SchemaTest(unittest.TestCase):
    def setUp(self):
        self.db = migrate()

    def tearDown(self):
        self.db.close()

    def test_migration_preserves_profile_and_membership(self):
        self.assertEqual(self.db.execute('SELECT id,displayName,usernameNormalized,createdAt FROM users').fetchall(), [('a','Alice','alice',100)])
        self.assertEqual(self.db.execute('SELECT userId,role FROM connection_members').fetchall(), [('a','MEMBER')])
        self.assertEqual(self.db.execute('SELECT status FROM connections').fetchone()[0], 'CLOSED')
        self.assertNotIn('email', [c[1] for c in self.db.execute('PRAGMA table_info(users)')])
        self.assertEqual(self.db.execute('PRAGMA foreign_key_check').fetchall(), [])
        self.assertEqual(self.db.execute("SELECT name FROM sqlite_master WHERE name='connection_requests'").fetchall(), [])

    def test_room_generated_schema_matches_migration(self):
        generated = DB / 'build/generated/ksp/debug/kotlin/com/tangai/memento/database/MementoDatabase_Impl.kt'
        self.assertTrue(generated.exists(), 'Run ./gradlew :app:assembleDebug first')
        expected = sqlite3.connect(':memory:')
        for sql in re.findall(r'(CREATE (?:TABLE|(?:UNIQUE )?INDEX) IF NOT EXISTS .*?)"', generated.read_text()):
            expected.execute(sql)
        for table in ['users','connections','connection_members','posts','media_items']:
            self.assertEqual(sorted(self.db.execute(f'PRAGMA table_info({table})').fetchall()),
                             sorted(expected.execute(f'PRAGMA table_info({table})').fetchall()), table)
            self.assertEqual(self.db.execute(f'PRAGMA foreign_key_list({table})').fetchall(),
                             expected.execute(f'PRAGMA foreign_key_list({table})').fetchall(), table)
            actual_indexes = {row[1] for row in self.db.execute(f'PRAGMA index_list({table})') if row[3] == 'c'}
            expected_indexes = {row[1] for row in expected.execute(f'PRAGMA index_list({table})') if row[3] == 'c'}
            self.assertEqual(actual_indexes, expected_indexes)
        expected.close()

    def query(self, **overrides):
        sql = re.search(r'@Query\("""(.*?)"""\)', (SRC / 'dao/PostDao.kt').read_text(), re.S).group(1)
        args = dict(currentUserId='a', connectionId=None, authorFilter='ALL', postType=None, fromTime=None, toTime=None)
        args.update(overrides)
        return [row[0] for row in self.db.execute(sql, args)]

    def seed_posts(self):
        self.db.execute("UPDATE connections SET status='ACTIVE'")
        for post_id, author, kind, created, client, status in [
            ('old','a','PHOTO',100,100,'ACTIVE'),
            ('received','b','VIDEO',200,200,'ACTIVE'),
            ('pending','a','PHOTO',None,300,'ACTIVE'),
            ('deleted','a','PHOTO',400,400,'DELETED')]:
            self.db.execute('INSERT INTO posts VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
                            (post_id,'c',author,kind,'SINGLE',None,client,created,created,status,None,None,1,'PENDING' if created is None else 'SYNCED'))

    def test_pending_order_and_filters(self):
        self.seed_posts()
        self.assertEqual(self.query(), ['pending','received','old'])
        self.assertEqual(self.query(authorFilter='MY'), ['pending','old'])
        self.assertEqual(self.query(authorFilter='RECEIVED'), ['received'])
        self.assertEqual(self.query(postType='VIDEO'), ['received'])
        self.assertEqual(self.query(fromTime=150,toTime=250), ['received'])
        self.assertEqual(self.query(connectionId='other'), [])

    def test_account_and_revocation_guards(self):
        self.seed_posts()
        self.assertEqual(self.query(currentUserId='b'), [])
        self.db.execute("UPDATE connection_members SET status='LEFT'")
        self.assertEqual(self.query(), [])
        self.db.execute("UPDATE connection_members SET status='ACTIVE'")
        self.db.execute("UPDATE connections SET status='CLOSED'")
        self.assertEqual(self.query(), [])

    def test_post_ids_are_scoped_by_connection(self):
        self.seed_posts()
        self.db.execute("INSERT INTO posts SELECT id,'another',authorId,postType,layoutType,caption,clientCreatedAt,createdAt,updatedAt,status,deletedAt,deletedBy,schemaVersion,localSyncStatus FROM posts WHERE id='old'")
        self.assertEqual(self.db.execute("SELECT count(*) FROM posts WHERE id='old'").fetchone()[0], 2)


if __name__ == '__main__':
    unittest.main(verbosity=2)
