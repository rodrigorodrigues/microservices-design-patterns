# Android WebApp Feature Parity with React WebApp

## Goal
Bring `android-webapp` to feature parity with `react-webapp` for the four gaps identified by comparing both codebases: no Passkey/WebAuthn support, no admin "Create All" (Person+Task) wizard, no UI for a Person's `children` (modeled but unused), and no way to assign Products to a WeekMenu Category. Also remove a stale leftover file (`UserFormActivity.java.bak`).

## Problem Description
1. **WebAuthn / Passkeys — entirely missing.** `react-webapp/src/passkeys/PasskeyList.js` and `PasskeyEdit.js` let a user list, register, and delete passkeys, and `react-webapp/src/login/Login.js` (`handlePasskey`, line 67) offers "Sign in with a passkey" using `@passwordless-id/webauthn`. `android-webapp` has no `Passkey` model, no API service, and `LoginActivity.java` only supports username/password and Google OAuth2 (Chrome Custom Tab).
2. **Admin "Create All" wizard — entirely missing.** `react-webapp/src/admin/CreateAll.js` is a `ROLE_ADMIN`-gated screen that creates a Person and a Task together. Note: despite its `task` state object carrying `fullName`/`dateOfBirth`/`address` fields, it only ever `POST`s to `/api/tasks` — this is legacy/inconsistent in react-webapp itself. The real backend contract (confirmed via `android-webapp`'s existing `Task.java` model) is that a `Task` has a `personId` reference, not embedded person fields, so the correct implementation is two calls: create the `Person` first, then create the `Task` with the returned `personId`. `android-webapp` has no equivalent screen or `drawer_menu.xml` entry.
3. **Person → Children — modeled but read-only, and entirely absent from Android.** Contrary to the initial gap survey, `react-webapp`'s own `PersonEdit.js` has children creation commented out (lines 22-25) — react-webapp does not support adding/editing children at all. The only children UI in react-webapp is a **read-only** view: `react-webapp/src/person/child/ChildModal.js`, opened via a "Show" button from `react-webapp/src/person/PersonList.js:191` (`{person.children ? <ChildModal person={person} /> : 'No Child'}`). `android-webapp/app/src/main/java/com/springboot/android/model/Person.java` already deserializes `children` (a `List<Children>` with `name`/`dateOfBirth`), but neither `PersonAdapter.java` nor `PersonListActivity.java` nor `PersonFormActivity.java` reference it. Parity means adding the same **read-only** children view to the Android person list, not a create/edit UI.
4. **WeekMenu Category — can't assign products.** `react-webapp/src/WeekMenu/CategoryEdit.js` includes a `products` text field (line 118, 136) as part of the category form. `android-webapp`'s `Category.java` model already has `List<CategoryProduct> products`, but `CategoryFormActivity.java` only exposes `etName` — there's no way to assign products to a category from the app.
5. **Stale file.** `android-webapp/app/src/main/java/com/springboot/android/ui/UserFormActivity.java.bak` is an older, simpler version of `UserFormActivity.java` (plain username/email/activated-checkbox, no dynamic permission checkboxes). The live `UserFormActivity.java` already implements the same dynamic `ROLE_*` checkbox UI that `react-webapp/src/user/UserEdit.js`'s permission `Switch` toggles provide — the `.bak` file is dead weight and should be deleted.

## Proposed Solution

### 1. Delete stale `.bak` file
Remove `android-webapp/app/src/main/java/com/springboot/android/ui/UserFormActivity.java.bak`. No functional change — the live file already has full parity.

### 2. Person → Children (read-only)
- Add a "Show Children" `MaterialButton` to `item_person.xml`, visible only when `person.getChildren()` is non-empty (mirrors react's `{person.children ? <ChildModal .../> : 'No Child'}` conditional).
- In `PersonAdapter.java`, wire the button's click to a callback that shows an `AlertDialog` listing each child's `name` and `dateOfBirth` (a simple two-column list), matching the `ChildModal` table columns ("Name", "Date of Birth"). No new Activity is needed — a dialog is the closest equivalent to react's `Modal`.
- No API or model changes: `Person.Children` already exists and is already populated by `PersonListActivity`'s existing `getPersons` call.

### 3. WeekMenu Category → Product assignment
- Reuse the existing `List<CategoryProduct> products` field on `Category.java` (no model change needed).
- In `CategoryFormActivity.java`, add a product multi-select section using the same dynamic-checkbox pattern `UserFormActivity.java` already uses for permissions (`LinearLayout` container + `Map<String, CheckBox>`), populated from `ProductService.getProducts(0, 100, null)` instead of a fixed permission list.
- On save, build `List<CategoryProduct>` from the checked products (`name` from the checked `Product`, `quantity` defaulted to `1`, `completed` defaulted to `false`) and set it on the `Category` before calling `createCategory`/`updateCategory`.
- On edit-mode load, pre-check any checkbox whose product name matches an existing `CategoryProduct.getName()`.

### 4. Admin "Create All" wizard
- New `CreateAllActivity.java`, gated by `ROLE_ADMIN` only (not the general `PermissionHelper.hasAnyPermission`, which also passes for any `SCOPE_openid` user — react's `CreateAll.js` checks specifically `permissions.some(item => item === 'ROLE_ADMIN')`). Add a dedicated `PermissionHelper.isAdmin(authorities)` helper for this stricter check.
- Layout `activity_create_all.xml` follows `activity_person_form.xml`/`activity_category_form.xml` conventions: two stacked `MaterialCardView` sections ("Add Person": full name, date of birth, address fields; "Add Task": task name, optional post autocomplete), plus a single "Create" button and `ProgressBar`.
- On submit: `personService.createPerson(person)` first; on success, take the returned `Person.getId()` and call `taskService.createTask(task)` with `task.setPersonId(id)` (and `postId` if a post was selected). This matches the real `Task`/`Person` API contract rather than react's inconsistent combined-object submission.
- Add a `nav_create_all` item to `drawer_menu.xml` under a new "Admin" group, and gate its visibility in `DashboardActivity.configureMenuItemsVisibility()` using the new `PermissionHelper.isAdmin(authorities)`.

### 5. WebAuthn / Passkeys
- **Dependencies:** add `androidx.credentials:credentials:1.3.0` and `androidx.credentials:credentials-play-services-auth:1.3.0` to `android-webapp/app/build.gradle` — no FIDO2/Credential Manager dependency currently exists in the project. Use Android's `CredentialManager` API (the current recommended approach, superseding the older `Fido2ApiClient`) for both passkey registration and sign-in.
- **Model:** new `Passkey.java` (`id`, `label`, `created`, `lastUsed`, `signatureCount`, `lastModifiedByUser`, `lastModifiedDate`) mirroring the columns in `PasskeyList.js`'s table.
- **API:** new `PasskeyService.java` retrofit interface:
  - `GET api/webauthns` → `Call<List<Passkey>>` (list)
  - `DELETE api/webauthns/{id}` → `Call<Void>` (delete)
  - Registration/authentication challenge exchange happens over raw `POST webauthn/register/options`, `POST webauthn/register`, `POST webauthn/authenticate/options`, `POST login/webauthn` — same endpoints react-webapp calls — added as additional methods on `PasskeyService`/`AuthService` returning raw JSON (`Call<JsonObject>` via `com.google.gson.JsonObject`) since the exact challenge/credential payload shapes are WebAuthn-spec JSON, not fixed DTOs.
- **UI:** `PasskeyListActivity.java` + `item_passkey.xml`/`PasskeyAdapter.java` (list + delete), and `PasskeyFormActivity.java` for registering a new passkey (label input + "Register" button that triggers `CredentialManager.createCredential(...)` with a `CreatePublicKeyCredentialRequest` built from the server's challenge).
- **Login integration:** add a "Sign in with a passkey" `MaterialButton` to `activity_login.xml` and `LoginActivity.java`, calling `CredentialManager.getCredential(...)` with a `GetPublicKeyCredentialOption` built from `webauthn/authenticate/options`, then posting the assertion to `login/webauthn` the same way `Login.js`'s `handlePasskey()` does.
- Add `nav_passkeys` to `drawer_menu.xml` (no special role required — passkeys are a self-service feature, same as react-webapp, which only gates on `isAuthenticated`).

## Architecture & Components
- **PermissionHelper:** add `isAdmin(List<String> authorities)` for the stricter ROLE_ADMIN-only check used by Create All.
- **PersonAdapter / item_person.xml:** add read-only children dialog trigger.
- **CategoryFormActivity / activity_category_form.xml:** add dynamic product checkboxes, reusing the `UserFormActivity` pattern.
- **CreateAllActivity (new) / activity_create_all.xml (new):** chains `PersonService.createPerson` → `TaskService.createTask`.
- **PasskeyService (new), Passkey (new), PasskeyListActivity/PasskeyAdapter/PasskeyFormActivity (new):** WebAuthn registration/list/delete via `androidx.credentials`.
- **LoginActivity / activity_login.xml:** add passkey sign-in entry point.
- **DashboardActivity / drawer_menu.xml:** add `nav_create_all` (ROLE_ADMIN-gated) and `nav_passkeys` (self-service) navigation entries.
- **Deleted:** `UserFormActivity.java.bak`.

## Testing Strategy
- `android-webapp` currently has no unit or instrumentation tests under `app/src/test` or `app/src/androidTest` despite `junit`/`espresso` test dependencies being declared — there is no existing test suite to extend. Verification for this work is a build check per task: `./gradlew assembleDebug` (or `./gradlew :app:assembleDebug` from the repo root) must succeed after each task.
- Manual verification checklist (since there's no CI/emulator harness in this repo for android-webapp):
  - Person list shows "Show Children" only for persons with children; dialog lists correct name/DOB pairs.
  - Category form shows one checkbox per product; saving persists the checked set; re-opening for edit pre-checks the right ones.
  - `nav_create_all` is only enabled for `ROLE_ADMIN`; submitting creates a Person then a Task referencing it.
  - Passkey registration round-trips through `webauthn/register/options` → `CredentialManager.createCredential` → `webauthn/register`; passkey login round-trips through `webauthn/authenticate/options` → `CredentialManager.getCredential` → `login/webauthn`.
