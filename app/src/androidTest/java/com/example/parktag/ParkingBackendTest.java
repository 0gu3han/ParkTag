package com.example.parktag;

import android.content.Context;
import android.os.Looper;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.parktag.data.ParkingDatabase;
import com.example.parktag.data.ParkingRepository;
import com.example.parktag.data.ParkingSessionDao;
import com.example.parktag.model.ParkingSession;
import com.example.parktag.service.ParkingService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ParkingBackendTest {

    private ParkingDatabase database;
    private ParkingSessionDao dao;
    private ParkingRepository repository;
    private ParkingService service;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();

        // Separate from the app's saved parking data.
        database = Room.inMemoryDatabaseBuilder(
                context,
                ParkingDatabase.class
        ).build();

        dao = database.parkingSessionDao();
        repository = new ParkingRepository(dao);
        service = new ParkingService(repository);
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void saveAndRead_preservesParkingDetails() throws Exception {
        ParkingSession original = newSession();
        original.setLevel("3");
        original.setSection("B");
        original.setNote("Near the elevator");

        Result<Long> saved = await(
                callback -> service.saveSession(original, callback)
        );

        assertNull(saved.error);
        assertNotNull(saved.value);
        assertTrue(saved.value > 0);
        assertEquals(saved.value.longValue(), original.getId());

        Result<ParkingSession> loaded = await(
                callback -> service.getSession(saved.value, callback)
        );

        assertNull(loaded.error);
        assertNotNull(loaded.value);

        ParkingSession actual = loaded.value;

        assertEquals(original.getLatitude(), actual.getLatitude(), 0.000001);
        assertEquals(original.getLongitude(), actual.getLongitude(), 0.000001);
        assertEquals(original.getAccuracyMeters(), actual.getAccuracyMeters());
        assertEquals(original.getCapturedAt(), actual.getCapturedAt());
        assertEquals("3", actual.getLevel());
        assertEquals("B", actual.getSection());
        assertEquals("Near the elevator", actual.getNote());
        assertTrue(actual.getCreatedAt() > 0);
        assertEquals(actual.getCreatedAt(), actual.getUpdatedAt());
        assertNull(actual.getClosedAt());
    }

    @Test
    public void emptyHistory_isSuccessfulEmptyList() throws Exception {
        Result<List<ParkingSession>> result = await(service::getHistory);

        assertNull(result.error);
        assertNotNull(result.value);
        assertTrue(result.value.isEmpty());
    }

    @Test
    public void missingRecord_isNullWithoutDatabaseError() throws Exception {
        Result<ParkingSession> result = await(
                callback -> service.getSession(999L, callback)
        );

        assertNull(result.error);
        assertNull(result.value);
    }

    @Test
    public void history_ordersByCreatedTimeThenId() throws Exception {
        ParkingSession older = newSession();
        older.setCreatedAt(1000L);

        ParkingSession newer = newSession();
        newer.setCreatedAt(2000L);

        ParkingSession sameTimeInsertedLast = newSession();
        sameTimeInsertedLast.setCreatedAt(2000L);

        // Fixture setup runs on the test thread, not the UI thread.
        long olderId = dao.insert(older);
        long newerId = dao.insert(newer);
        long lastId = dao.insert(sameTimeInsertedLast);

        Result<List<ParkingSession>> result = await(service::getHistory);

        assertNull(result.error);
        assertEquals(3, result.value.size());
        assertEquals(lastId, result.value.get(0).getId());
        assertEquals(newerId, result.value.get(1).getId());
        assertEquals(olderId, result.value.get(2).getId());
    }

    @Test
    public void closeSession_clearsReminderAndOnlyClosesOnce() throws Exception {
        ParkingSession session = newSession();
        session.setCreatedAt(1000L);
        session.setUpdatedAt(1000L);

        // Seed reminder metadata directly to test the close query.
        // This does not schedule an Android alarm.
        session.setReminderAt(System.currentTimeMillis() + 60000L);
        long id = dao.insert(session);

        Result<Boolean> firstClose = await(
                callback -> service.closeSession(id, callback)
        );

        assertNull(firstClose.error);
        assertEquals(Boolean.TRUE, firstClose.value);

        Result<ParkingSession> loaded = await(
                callback -> service.getSession(id, callback)
        );

        assertNull(loaded.error);
        assertNotNull(loaded.value.getClosedAt());
        assertNull(loaded.value.getReminderAt());
        assertEquals(
                loaded.value.getClosedAt().longValue(),
                loaded.value.getUpdatedAt()
        );

        Result<Boolean> secondClose = await(
                callback -> service.closeSession(id, callback)
        );

        assertNull(secondClose.error);
        assertEquals(Boolean.FALSE, secondClose.value);

        Result<Boolean> missingClose = await(
                callback -> service.closeSession(999L, callback)
        );

        assertNull(missingClose.error);
        assertEquals(Boolean.FALSE, missingClose.value);
    }

    @Test
    public void databaseFailures_reachEveryRepositoryCallback() throws Exception {
        FailingDao failingDao = new FailingDao();
        ParkingRepository failingRepository =
                new ParkingRepository(failingDao);

        assertFailure(
                this.<Long>await(callback ->
                        failingRepository.save(newSession(), callback)),
                failingDao.failure
        );

        assertFailure(
                this.<ParkingSession>await(callback ->
                        failingRepository.getById(1L, callback)),
                failingDao.failure
        );

        assertFailure(
                this.<List<ParkingSession>>await(
                        failingRepository::getHistory),
                failingDao.failure
        );

        assertFailure(
                this.<Boolean>await(callback ->
                        failingRepository.closeSession(1L, callback)),
                failingDao.failure
        );

        assertFailure(
                this.<Void>await(callback ->
                        failingRepository.update(newSession(), callback)),
                failingDao.failure
        );

        assertFalse(
                "DAO operations must run off the main thread",
                failingDao.ranOnMainThread
        );

        // The shared worker must still accept work after failures.
        Result<List<ParkingSession>> recovery =
                await(repository::getHistory);

        assertNull(recovery.error);
        assertTrue(recovery.value.isEmpty());
    }

    @Test
    public void invalidInputs_areRejectedWithoutSaving() throws Exception {
        ParkingSession badCoordinates = newSession();
        badCoordinates.setLatitude(Double.NaN);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.saveSession(
                        badCoordinates,
                        (value, error) -> fail("Invalid input reached callback")
                )
        );

        ParkingSession longNote = newSession();
        longNote.setNote(noteOfLength(281));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.saveSession(
                        longNote,
                        (value, error) -> fail("Invalid input reached callback")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getSession(0L, (value, error) -> {})
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.closeSession(-1L, (value, error) -> {})
        );

        Result<List<ParkingSession>> history = await(service::getHistory);

        assertNull(history.error);
        assertTrue(history.value.isEmpty());
    }

    @Test
    public void noteOfExactly280Characters_isAccepted() throws Exception {
        ParkingSession session = newSession();
        session.setNote(noteOfLength(280));

        Result<Long> saved = await(
                callback -> service.saveSession(session, callback)
        );

        assertNull(saved.error);
        assertNotNull(saved.value);
        assertTrue(saved.value > 0);
    }

    private ParkingSession newSession() {
        ParkingSession session = new ParkingSession();
        session.setLatitude(35.848);
        session.setLongitude(-86.365);
        session.setAccuracyMeters(5.0f);
        session.setCapturedAt(System.currentTimeMillis());
        return session;
    }

    private String noteOfLength(int length) {
        char[] characters = new char[length];
        Arrays.fill(characters, 'a');
        return new String(characters);
    }

    private void assertFailure(
            Result<?> result,
            Exception expectedError
    ) {
        assertNull("Failure must not return a success value", result.value);
        assertSame(expectedError, result.error);
    }

    private interface Operation<T> {
        void start(ParkingRepository.Callback<T> callback);
    }

    private static class Result<T> {
        T value;
        Exception error;
        boolean deliveredOnMainThread;
    }

    private <T> Result<T> await(Operation<T> operation) throws Exception {
        assertNotSame(
                "Do not block the UI thread while waiting",
                Looper.getMainLooper(),
                Looper.myLooper()
        );

        CountDownLatch completed = new CountDownLatch(1);
        Result<T> result = new Result<>();

        // Start the operation as an Activity would: on the UI thread.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                operation.start((value, error) -> {
                    result.value = value;
                    result.error = error;
                    result.deliveredOnMainThread =
                            Looper.myLooper() == Looper.getMainLooper();
                    completed.countDown();
                })
        );

        assertTrue(
                "Repository callback did not arrive within 5 seconds",
                completed.await(5, TimeUnit.SECONDS)
        );

        assertTrue(
                "Callback must be delivered on the main thread",
                result.deliveredOnMainThread
        );

        return result;
    }

    private static class FailingDao implements ParkingSessionDao {

        final RuntimeException failure =
                new IllegalStateException("Simulated database failure");

        volatile boolean ranOnMainThread;

        private RuntimeException failure() {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                ranOnMainThread = true;
            }
            return failure;
        }

        @Override
        public long insert(ParkingSession session) {
            throw failure();
        }

        @Override
        public void update(ParkingSession session) {
            throw failure();
        }

        @Override
        public ParkingSession getById(long id) {
            throw failure();
        }

        @Override
        public List<ParkingSession> getNewestFirst() {
            throw failure();
        }

        @Override
        public int closeSession(long id, long timeMillis) {
            throw failure();
        }
    }
}