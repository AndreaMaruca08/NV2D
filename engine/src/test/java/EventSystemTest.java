import nv.core.events.EventSystem;
import nv.core.events.NvEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class EventSystemTest {

    private final EventSystem eventSystem = new EventSystem();
    record TestEvent(String message) implements NvEvent {}
    record AnotherEvent(int value) implements NvEvent {}

    @Test
    void shouldCallRegisteredListener() {
        AtomicReference<TestEvent> received = new AtomicReference<>();

        eventSystem.on(TestEvent.class, received::set);

        TestEvent event = new TestEvent("Hello");

        eventSystem.emit(TestEvent.class, event);

        Assertions.assertEquals(event, received.get());
    }


    @Test
    void shouldCallMultipleListeners() {
        AtomicInteger counter = new AtomicInteger();

        eventSystem.on(TestEvent.class, _ -> counter.incrementAndGet());
        eventSystem.on(TestEvent.class, _ -> counter.incrementAndGet());
        eventSystem.on(TestEvent.class, _ -> counter.incrementAndGet());

        eventSystem.emit(TestEvent.class, new TestEvent("Hello"));

        Assertions.assertEquals(3, counter.get());
    }


    @Test
    void shouldOnlyCallListenersForCorrectEventType() {
        AtomicInteger testEventCounter = new AtomicInteger();
        AtomicInteger anotherEventCounter = new AtomicInteger();

        eventSystem.on(TestEvent.class, _ -> testEventCounter.incrementAndGet());
        eventSystem.on(AnotherEvent.class, _ -> anotherEventCounter.incrementAndGet());
        eventSystem.emit(TestEvent.class, new TestEvent("Hello"));

        Assertions.assertEquals(1, testEventCounter.get());
        Assertions.assertEquals(0, anotherEventCounter.get());
    }

    @Test
    void shouldPassCorrectEventToListener() {
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        eventSystem.on(TestEvent.class, event -> receivedMessage.set(event.message()));
        eventSystem.emit(TestEvent.class, new TestEvent("Hello World"));

        Assertions.assertEquals("Hello World", receivedMessage.get());
    }


    @Test
    void shouldDoNothingWhenNoListenersAreRegistered() {
        Assertions.assertDoesNotThrow(() -> eventSystem.emit(TestEvent.class, new TestEvent("Hello")));
    }


    @Test
    void shouldKeepListenersRegisteredAfterEmit() {
        AtomicInteger counter = new AtomicInteger();

        eventSystem.on(TestEvent.class, _ -> counter.incrementAndGet());
        eventSystem.emit(TestEvent.class, new TestEvent("First"));
        eventSystem.emit(TestEvent.class, new TestEvent("Second"));
        eventSystem.emit(TestEvent.class, new TestEvent("Third"));

        Assertions.assertEquals(3, counter.get());
    }


    @Test
    void shouldSupportDifferentEventTypes() {
        AtomicReference<String> message = new AtomicReference<>();
        AtomicInteger value = new AtomicInteger();

        eventSystem.on(TestEvent.class, event -> message.set(event.message()));
        eventSystem.on(AnotherEvent.class, event -> value.set(event.value()));
        eventSystem.emit(TestEvent.class, new TestEvent("Hello"));
        eventSystem.emit(AnotherEvent.class, new AnotherEvent(42));
        eventSystem.emit(TestEvent.class, new TestEvent("Hello"));
        eventSystem.emit(AnotherEvent.class, new AnotherEvent(42));

        Assertions.assertEquals("Hello", message.get());
        Assertions.assertEquals(42, value.get());
    }


    @Test
    void shouldAllowSameListenerToBeRegisteredMultipleTimes() {
        AtomicInteger counter = new AtomicInteger();

        var listener = (java.util.function.Consumer<TestEvent>) _ -> counter.incrementAndGet();

        eventSystem.on(TestEvent.class, listener);
        eventSystem.on(TestEvent.class, listener);

        eventSystem.emit(TestEvent.class, new TestEvent("Hello"));

        Assertions.assertEquals(2, counter.get());
    }


    @Test
    void shouldUseSnapshotWhenListenerAddsAnotherListener() {
        AtomicInteger counter = new AtomicInteger();

        eventSystem.on(TestEvent.class, _ -> {
            counter.incrementAndGet();

            eventSystem.on(TestEvent.class, ignored -> counter.incrementAndGet());
        });

        eventSystem.emit(TestEvent.class, new TestEvent("Hello"));

        Assertions.assertEquals(1, counter.get());

        eventSystem.emit(TestEvent.class, new TestEvent("Hello"));

        Assertions.assertEquals(3, counter.get());
    }
}
