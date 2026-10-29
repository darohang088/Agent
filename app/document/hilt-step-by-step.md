# Learn the app flow step by step

This guide uses the current Agent App as a working example. Read the lessons in order, open the linked source files, and answer each checkpoint before moving on. The examples explain the implementation; they do not add new screens or endpoints.

## Lesson 1 — Understand the four responsibilities

Start with one feature: login.

| Responsibility | Current file | Main job |
| --- | --- | --- |
| Connect state to UI | [LoginRoute.kt](../src/main/java/com/daro/agentapp/presentation/login/LoginRoute.kt) | Obtain the ViewModel, collect state, handle navigation callbacks |
| Render | [LoginScreen.kt](../src/main/java/com/daro/agentapp/presentation/login/LoginScreen.kt) | Display fields/buttons and report actions |
| Describe screen state | [LoginUiState.kt](../src/main/java/com/daro/agentapp/presentation/login/LoginUiState.kt) | Hold input, submitting, error, and signed-in state |
| Handle actions | [LoginViewModel.kt](../src/main/java/com/daro/agentapp/presentation/login/LoginViewModel.kt) | Validate input, call repositories, publish state |

Follow the arrows:

```text
User action → Screen callback → ViewModel → Repository
                                  ↓
Screen ← Route collects StateFlow ← UiState update
```

The Screen does not construct Retrofit or store tokens. The Route does not perform login business logic. The ViewModel does not need a NavController.

**Checkpoint:** Which file should change when you redesign the login button? Usually `LoginScreen.kt`. Which should change when submission behavior changes? Usually `LoginViewModel.kt`.

## Lesson 2 — Understand what Hilt supplies

A dependency is an object another object needs. Your login ViewModel needs an `AuthRepository` and a `SessionRepository`:

```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel()
```

Read this as: “Hilt may construct this ViewModel using these two repository dependencies.”

Hilt does not know which implementation an interface represents until you provide a binding. Open [AppModule.kt](../src/main/java/com/daro/agentapp/di/AppModule.kt) and find `authRepository()` and `sessionRepository()`.

```text
Requested type: AuthRepository
        ↓ Hilt provider
Actual object: DefaultAuthRepository
        ↓ requires
AuthApi → Retrofit → OkHttpClient
```

For an invalid API environment, the provider supplies `UnconfiguredAuthRepository` instead. Its `Provider<AuthApi>` parameter delays resolving the API, so invalid configuration does not try to construct Retrofit.

**Checkpoint:** Adding `@Inject` to a ViewModel does not implement an API or supply a missing repository binding.

## Lesson 3 — Learn the annotations used in this project

| Annotation/API | Meaning here |
| --- | --- |
| `@HiltAndroidApp` | Generate the application Hilt setup |
| `@AndroidEntryPoint` | Connect the activity to Hilt |
| `@HiltViewModel` | Register a ViewModel for Hilt creation |
| `@Inject constructor` | Declare dependencies used to construct an object |
| `@Inject` on an Application property | Request field injection from Hilt |
| `@Module` | Declare dependency bindings/providers |
| `@InstallIn(SingletonComponent::class)` | Install the module in the application component |
| `@Provides` | Describe how to create a dependency |
| `@Singleton` | Reuse one instance within the application component, across requests for that binding |
| `@ApplicationContext` | Request the application Context, not an activity Context |
| `@ApplicationScope` | This project's qualifier for the app-owned coroutine scope |
| `@Multibinds` | Declare the cleanup dependency set, initially empty |
| `hiltViewModel()` | Obtain a ViewModel using Hilt and the current ViewModelStoreOwner |

`@ApplicationScope` distinguishes a `CoroutineScope` binding; it is not itself a Hilt lifetime scope. `@Singleton` provides that binding's application lifetime. A singleton is not persistent storage and does not survive process death.

KSP processes annotations at build time. Hilt generates component and factory code. Do not edit generated files under `app/build/`.

**Checkpoint:** Which annotation means “use application Context”? `@ApplicationContext`.

## Lesson 4 — Trace application startup

Open these files in order:

1. [AgentApplication.kt](../src/main/java/com/daro/agentapp/AgentApplication.kt).
2. [AppModule.kt](../src/main/java/com/daro/agentapp/di/AppModule.kt).
3. [MainActivity.kt](../src/main/java/com/daro/agentapp/MainActivity.kt).
4. [AgentApp.kt](../src/main/java/com/daro/agentapp/app/AgentApp.kt).
5. [AppNavHost.kt](../src/main/java/com/daro/agentapp/navigation/AppNavHost.kt).

The runtime sequence is:

```text
Android creates AgentApplication
    ↓
attachBaseContext applies the selected language
    ↓
onCreate calls super; Hilt supplies injected fields
    ↓
Application scope launches sessionRepository.restore()
    ↓                         (asynchronous)
MainActivity starts and receives Hilt support
    ↓
setContent → AgentApp → AppNavHost
    ↓
Welcome destination → GreetingRoute → GreetingViewModel
```

Dependencies are resolved when requested; the entire network graph is not necessarily created at startup. Application injection requests the session repository and app scope. A later login/register ViewModel request resolves authentication dependencies.

Session restoration runs asynchronously and is synchronized inside the session repository. The current app still starts at Welcome; automatic session-aware routing is deferred.

**Checkpoint:** Does `@HiltAndroidApp` automatically navigate to Home after restoring a session? No. Navigation behavior must be implemented separately.

## Lesson 5 — Trace a login from tap to response

### Step 1: Open the destination

`AppNavHost` registers the login destination and calls `LoginRoute`.

### Step 2: Obtain the ViewModel

The Route's default parameter uses:

```kotlin
viewModel: LoginViewModel = hiltViewModel()
```

Inside a navigation destination, the current owner is normally its back-stack entry. The ViewModel is reused for that entry; recomposition does not create a new one every time. Once that entry is removed and its ViewModelStore cleared, the ViewModel is cleared too.

### Step 3: Supply dependencies

Hilt resolves `AuthRepository` and `SessionRepository` using its application providers. It then creates `LoginViewModel`. The repositories are shared; each destination's ViewModel owns its screen state separately.

### Step 4: Collect screen state

The Route uses `collectAsStateWithLifecycle()` and passes the state and callbacks to `LoginScreen`.

### Step 5: Receive a button tap

The Screen invokes the submit callback. `LoginViewModel.submit()` checks input and guards against duplicate submission, then updates `isSubmitting`.

### Step 6: Call the repository

The ViewModel creates a domain `LoginRequest` and calls `authRepository.login(request)` within `viewModelScope`.

### Step 7: Perform the request

Follow these source files:

- [DefaultAuthRepository.kt](../src/main/java/com/daro/agentapp/data/repository/DefaultAuthRepository.kt): validation and domain/DTO mapping.
- [AuthApi.kt](../src/main/java/com/daro/agentapp/data/remote/api/AuthApi.kt): `POST /api/v1/auth/login`.
- [RetrofitProvider.kt](../src/main/java/com/daro/agentapp/data/remote/RetrofitProvider.kt): converter and interceptors.
- [NetworkClient.kt](../src/main/java/com/daro/agentapp/data/remote/NetworkClient.kt): transport policy and timeouts.
- [BaseRepo.kt](../src/main/java/com/daro/agentapp/base/BaseRepo.kt): typed transport/response failures; cancellation propagates.

```text
LoginRequest → request DTO → JSON → backend
backend JSON → AuthResponseDto → validated AuthSession → AppResult
```

Hilt supplies the dependencies; Retrofit performs HTTP work. They have different jobs.

### Step 8: Establish the session

On authentication success, the ViewModel explicitly calls `sessionRepository.establish(result.value)`. The repository validates the session and writes it through [KeystoreSessionStore.kt](../src/main/java/com/daro/agentapp/data/local/session/KeystoreSessionStore.kt).

Only successful establishment sets `isSignedIn = true`. A storage failure produces an error instead. `DefaultAuthRepository` does not automatically persist sessions.

### Step 9: Render or navigate

The Route observes the updated screen state. Its effect invokes `onSignedIn()`, and the app navigation callback opens Home. A failure remains on the form and renders the corresponding error state.

This UI login-success state is distinct from the global session repository's `StateFlow`. Global session-aware restoration/expiry navigation is not implemented yet.

**Checkpoint:** Does a successful HTTP login always mean Home opens? No. Response validation and secure session establishment must also succeed.

## Lesson 6 — Add a new feature in order

Use this sequence when a real feature contract is available. `Profile` is a teaching name, not an endpoint already implemented in this project.

1. **Define behavior:** loading, success, empty, failure, retry, and any permission requirements.
2. **Define a domain contract:** add the model and repository interface if this feature needs new data operations. Reuse an existing contract where appropriate.
3. **Implement data access:** add the actual endpoint/DTO, mapper, and repository implementation. Reuse the shared client and typed error handling. Do not invent backend fields or paths.
4. **Provide the repository:** add a Hilt provider/binding in `di/`. Choose the lifetime deliberately; do not make every object a singleton by habit.
5. **Create the presentation files:** `ProfileUiState.kt`, `ProfileViewModel.kt`, `ProfileScreen.kt`, and `ProfileRoute.kt` when those responsibilities are needed.
6. **Inject dependencies:** annotate the ViewModel with `@HiltViewModel` and its constructor with `@Inject`.
7. **Connect the Route:** obtain it with `hiltViewModel()`, collect lifecycle-aware state, and pass callbacks to the Screen.
8. **Register navigation:** add a destination and app-owned navigation callback. Pass small identifiers, not credentials or entire responses.
9. **Add resources:** English/Khmer strings and shared images in `main/res/drawable/`.
10. **Verify:** unit-test meaningful state/error behavior, exercise navigation, and test the optimized release when the feature affects serialization, dependency injection, or SDK integration.

For a concrete class that you own, constructor injection may remove the need for a provider. For an interface, Hilt still needs a binding to an implementation. Existing runtime-dependent construction, such as Retrofit creation, belongs in providers.

**Checkpoint:** You no longer add `profileViewModelFactory` to `AppContainer`. That container and the manual factories have been removed.

## Lesson 7 — Test without making everything depend on Hilt

JVM tests can still construct a ViewModel directly:

```kotlin
val viewModel = LoginViewModel(fakeAuthRepository, fakeSessionRepository)
```

The fake variables above represent test implementations of the repository interfaces. No Android Hilt component is needed for that unit test.

Use UI tests for screen behavior and navigation; they verify the production Hilt application setup when launching `MainActivity`. If you need to replace Hilt bindings in device tests later, use Hilt's test component/module replacement support rather than editing production providers.

Commands:

```bash
python3 scripts/check_foundation.py
./gradlew :app:testStagingDebugUnitTest
./gradlew :app:connectedStagingDebugAndroidTest
./gradlew :app:assembleProdRelease
```

Device tests require a connected emulator/device. Release authentication still requires working production HTTPS; passing local tests does not establish backend availability.

**Exercise:** Read [AppModuleTest.kt](../src/test/java/com/daro/agentapp/di/AppModuleTest.kt). Explain why its API provider deliberately throws if resolved, and why the test must still pass for an invalid environment.

## Lesson 8 — Diagnose common mistakes

| Symptom | Check |
| --- | --- |
| Hilt reports a missing binding | Does the constructor dependency have an injectable constructor or a provider/binding? |
| Hilt reports duplicate bindings | Are two unqualified providers returning the same type? |
| An injected property is not initialized | Is its Android owner a supported Hilt entry point, and has injection occurred? |
| ViewModel creation fails in navigation | Is the activity a Hilt entry point and the ViewModel annotated correctly? |
| Screen state is unexpectedly shared | Which ViewModelStoreOwner/back-stack entry is being used? |
| API URL is invalid | Check `BuildConfig.API_BASE_URL` and `EnvironmentConfig`; do not bypass TLS checks |
| A preview tries to create real dependencies | Preview the Screen with supplied state/callbacks instead of a Hilt-backed Route |
| Release fails but debug works | Inspect R8 diagnostics and SDK consumer rules; add narrow rules only for verified requirements |

## Reading order after these lessons

- [Dependency ownership and scopes](dependency-injection.md)
- [Project overview](../../README.md)
- [Release validation and deferred acceptance work](release-validation.md)
- [Implementation history](implement.md)

Earlier PNG diagrams that mention `AppContainer` show the previous wiring. Use this lesson and the current source as the Hilt reference. The deferred session-aware routing, logout UI, production TLS fix, and authenticated-request/refresh work remain separate feature work.
