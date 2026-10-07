# MoneyTracker AI assistant

The assistant uses Firebase AI Logic with the Gemini Developer API and `gemini-3.5-flash-lite`.
The existing Firebase project configuration is reused. No separate Gemini API key belongs in the Android app.

## Enable the service

1. Open the existing project in Firebase Console.
2. Open **Firebase AI Logic**, select **Get started**, and choose **Gemini Developer API**.
3. Complete the guided API setup. If it produces a new Android configuration, replace `app/google-services.json` with the downloaded configuration.
4. Build and install the app. Open **AI assistant**, choose **Use example**, then **Create draft**.
5. Review the fields in the transaction form; saving is a separate explicit action.

If App Check enforcement is enabled for AI Logic, configure the project's App Check provider for the Android app and register the debug build appropriately. Before distributing publicly, configure production App Check and service quota controls.

## Behavior

- Draft requests send only the text entered, current date, and default currency.
- Explanations send aggregated monthly income, expenses, category totals, the previous month's expense total, and the currency's budget. Individual notes, transaction IDs, and account details are omitted.
- Changing text invalidates its draft; changing currency invalidates both results. Changing month invalidates the explanation.
- AI output never writes to Firestore. Currency, category, date, type, and exact monetary amount are validated before opening the review form.
- Service errors and requests exceeding 45 seconds leave the original records unchanged and allow retry.

Official setup: https://firebase.google.com/docs/ai-logic/get-started?api=dev&platform=android
Structured output: https://firebase.google.com/docs/ai-logic/generate-structured-output
