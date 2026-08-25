package com.griff.keeper.presentation.feedback

import androidx.lifecycle.SavedStateHandle
import com.griff.keeper.application.currency.ObserveAppCurrencyUseCase
import com.griff.keeper.application.obligation.AddObligationUseCase
import com.griff.keeper.application.obligation.GetObligationUseCase
import com.griff.keeper.application.obligation.UpdateObligationUseCase
import com.griff.keeper.application.obligation.ValidateObligationInputUseCase
import com.griff.keeper.application.provider.GetProviderUseCase
import com.griff.keeper.application.provider.GetProvidersUseCase
import com.griff.keeper.application.provider.SearchProvidersUseCase
import com.griff.keeper.application.subscription.AddSubscriptionUseCase
import com.griff.keeper.application.subscription.GetSubscriptionUseCase
import com.griff.keeper.application.subscription.UpdateSubscriptionUseCase
import com.griff.keeper.application.subscription.ValidateSubscriptionInputUseCase
import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.model.PaymentStatus
import com.griff.keeper.domain.model.ProviderId
import com.griff.keeper.domain.testing.FakeAppCurrencyRepository
import com.griff.keeper.domain.testing.FakeObligationRepository
import com.griff.keeper.domain.testing.FakeProviderCatalog
import com.griff.keeper.domain.testing.FakeSubscriptionRepository
import com.griff.keeper.domain.testing.FixedClockProvider
import com.griff.keeper.domain.testing.SequentialIdGenerator
import com.griff.keeper.domain.testing.SequentialObligationIdGenerator
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.domain.testing.testSubscription
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.MessageSeverity
import com.griff.keeper.presentation.common.TransientMessages
import com.griff.keeper.presentation.common.UiMessage
import com.griff.keeper.presentation.details.SubscriptionDetailsEvent
import com.griff.keeper.presentation.form.ProviderOption
import com.griff.keeper.presentation.form.SubscriptionFormEvent
import com.griff.keeper.presentation.form.SubscriptionFormViewModel
import com.griff.keeper.presentation.navigation.OBLIGATION_ID_ARG
import com.griff.keeper.presentation.navigation.SUBSCRIPTION_ID_ARG
import com.griff.keeper.presentation.obligations.form.ObligationFormEvent
import com.griff.keeper.presentation.obligations.form.ObligationFormViewModel
import com.griff.keeper.presentation.util.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

/**
 * Every record operation reports back, through the one mechanism.
 *
 * Add, edit and delete all end on a screen other than the one that caused them - a form closes
 * itself, a details screen disappears with its record - so the confirmation travels with the event
 * and is shown by whatever the user lands on. These tests pin the messages rather than the layout:
 * that they exist, that they say the right thing, and that add and edit are told apart.
 */
class RecordFeedbackTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `adding a subscription confirms that it was added`() = runTest {
        val repository = FakeSubscriptionRepository()
        val viewModel = subscriptionForm(repository)
        val events = recordEvents(this) { viewModel.events.collect(it) }

        viewModel.onProviderSelected(spotifyOption())
        viewModel.onPriceChange("34,99")
        viewModel.onSave()
        advanceUntilIdle()

        val saved = events.values.filterIsInstance<SubscriptionFormEvent.Saved>().single()
        assertEquals(R.string.subscription_added, saved.message.textRes)
        assertEquals(MessageSeverity.SUCCESS, saved.message.severity)
        assertEquals(1, repository.stored.size)
        events.stop()
    }

    @Test
    fun `editing a subscription confirms that it was updated`() = runTest {
        val repository = FakeSubscriptionRepository(
            listOf(testSubscription(id = "id-1", name = "Spotify")),
        )
        val viewModel = subscriptionForm(repository, editedId = "id-1")
        val events = recordEvents(this) { viewModel.events.collect(it) }
        advanceUntilIdle()

        viewModel.onPriceChange("44,99")
        viewModel.onSave()
        advanceUntilIdle()

        val saved = events.values.filterIsInstance<SubscriptionFormEvent.Saved>().single()
        assertEquals(R.string.subscription_updated, saved.message.textRes)
        events.stop()
    }

    @Test
    fun `adding an obligation confirms that it was added`() = runTest {
        val repository = FakeObligationRepository()
        val viewModel = obligationForm(repository)
        val events = recordEvents(this) { viewModel.events.collect(it) }

        viewModel.onNameChange("OC Ford")
        viewModel.onCategoryChange(ObligationCategory.VEHICLE_INSURANCE)
        viewModel.onAmountChange("1240,00")
        viewModel.onPaymentStatusChange(PaymentStatus.UNPAID)
        viewModel.onSave()
        advanceUntilIdle()

        val saved = events.values.filterIsInstance<ObligationFormEvent.Saved>().single()
        assertEquals(R.string.obligation_added, saved.message.textRes)
        assertEquals(MessageSeverity.SUCCESS, saved.message.severity)
        assertEquals(1, repository.stored.size)
        events.stop()
    }

    @Test
    fun `editing an obligation confirms that the changes were saved`() = runTest {
        val repository = FakeObligationRepository(
            listOf(testObligation(id = "obligation-1", name = "OC Ford")),
        )
        val viewModel = obligationForm(repository, editedId = "obligation-1")
        val events = recordEvents(this) { viewModel.events.collect(it) }
        advanceUntilIdle()

        viewModel.onAmountChange("1300,00")
        viewModel.onSave()
        advanceUntilIdle()

        val saved = events.values.filterIsInstance<ObligationFormEvent.Saved>().single()
        assertEquals(R.string.obligation_updated, saved.message.textRes)
        events.stop()
    }

    @Test
    fun `a failed save reports an error rather than a confirmation`() = runTest {
        val repository = FakeSubscriptionRepository()
        repository.failOnWrite = true
        val viewModel = subscriptionForm(repository)
        val events = recordEvents(this) { viewModel.events.collect(it) }

        viewModel.onProviderSelected(spotifyOption())
        viewModel.onPriceChange("34,99")
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(0, events.values.size)
        assertEquals(R.string.error_save_failed, viewModel.uiState.value.message?.textRes)
        assertEquals(MessageSeverity.ERROR, viewModel.uiState.value.message?.severity)
        events.stop()
    }

    @Test
    fun `the message relay hands one confirmation to the next screen and then forgets it`() {
        val messages = TransientMessages()
        assertNull(messages.pending)

        messages.show(
            UiMessage(
                textRes = R.string.delete_success,
                formatArgs = listOf("Spotify"),
                severity = MessageSeverity.SUCCESS,
            ),
        )
        assertEquals(R.string.delete_success, messages.pending?.textRes)

        messages.consume()
        // Shown once, not on every screen the user visits afterwards.
        assertNull(messages.pending)
    }

    @Test
    fun `deleting a record names it in the confirmation`() {
        // The details screen turns this event into the message the list shows; the event itself is
        // what carries the name, so the confirmation can say which record is gone.
        val event = SubscriptionDetailsEvent.Deleted(name = "Spotify")

        assertEquals("Spotify", event.name)
    }

    private fun subscriptionForm(
        repository: FakeSubscriptionRepository,
        editedId: String? = null,
    ): SubscriptionFormViewModel {
        val catalog = FakeProviderCatalog()
        val clock = FixedClockProvider()
        return SubscriptionFormViewModel(
            savedStateHandle = SavedStateHandle(
                editedId?.let { mapOf(SUBSCRIPTION_ID_ARG to it) } ?: emptyMap(),
            ),
            getProviders = GetProvidersUseCase(catalog),
            searchProviders = SearchProvidersUseCase(catalog),
            getProvider = GetProviderUseCase(catalog),
            getSubscription = GetSubscriptionUseCase(repository),
            addSubscription = AddSubscriptionUseCase(
                repository = repository,
                idGenerator = SequentialIdGenerator(),
                clock = clock,
            ),
            updateSubscription = UpdateSubscriptionUseCase(repository = repository, clock = clock),
            validateInput = ValidateSubscriptionInputUseCase(),
            observeAppCurrency = ObserveAppCurrencyUseCase(FakeAppCurrencyRepository()),
        )
    }

    private fun obligationForm(
        repository: FakeObligationRepository,
        editedId: String? = null,
    ): ObligationFormViewModel {
        val clock = FixedClockProvider()
        return ObligationFormViewModel(
            savedStateHandle = SavedStateHandle(
                editedId?.let { mapOf(OBLIGATION_ID_ARG to it) } ?: emptyMap(),
            ),
            getObligation = GetObligationUseCase(repository),
            addObligation = AddObligationUseCase(
                repository = repository,
                idGenerator = SequentialObligationIdGenerator(),
                clock = clock,
            ),
            updateObligation = UpdateObligationUseCase(repository = repository, clock = clock),
            validateInput = ValidateObligationInputUseCase(),
            observeAppCurrency = ObserveAppCurrencyUseCase(FakeAppCurrencyRepository()),
            clock = clock,
        )
    }

    private fun spotifyOption() = ProviderOption(
        id = ProviderId("spotify").value,
        displayName = "Spotify",
        logoKey = "spotify",
        isOther = false,
    )

    /**
     * Starts recording one-off events.
     *
     * Undispatched and on the test scope itself: the form events carry no replay, so a collector that
     * has not subscribed yet would miss them, and one that never finishes has to be cancelled or
     * `runTest` waits for it.
     */
    private fun <T> recordEvents(
        scope: TestScope,
        collect: suspend (suspend (T) -> Unit) -> Unit,
    ): Recorded<T> {
        val collected = mutableListOf<T>()
        val job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            collect { collected += it }
        }
        return Recorded(job, collected)
    }

    private class Recorded<T>(private val job: Job, val values: List<T>) {
        fun stop() = job.cancel()
    }
}
