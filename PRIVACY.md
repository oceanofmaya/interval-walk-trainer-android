# Privacy Policy

**Last Updated:** October 2026

## Introduction

Interval Walk Trainer ("we," "our," or "the app") is committed to protecting your privacy. This Privacy Policy explains how we handle information when you use our Android application.

## Information We Collect

### No Personal Data Collection

Interval Walk Trainer does **not** collect personal information or send workout data to developer-operated servers. The phone app can use Android system services, Health Connect, and—when you install the optional companion—Google's Wear OS Data Layer to communicate with your paired watch.

### Local Data Storage

The app stores the following preferences locally on your device using Android's SharedPreferences:

- **Theme Preference**: Your choice of dark or light theme
- **Vibration Setting**: Whether vibration notifications are enabled
- **Voice Setting**: Whether voice notifications are enabled
- **Workout Metrics Setting**: Whether optional Health Connect workout metrics are enabled
- **Weekly Goal Settings**: Optional weekly targets and reminder preferences, including selected reminder days and time

Additionally, the app stores workout history and optional saved workout templates locally on your device using a local database:

- **Workout History**: Dates on which you completed workouts, the number of workouts completed per day, total minutes walked per day, and per-session Health Connect metrics when optional Workout Metrics are enabled and available. This includes statistics such as workout streaks, total workouts, total minutes walked, step counts, heart-rate summaries, and fast-vs-slow phase heart-rate summaries.
- **Saved Presets**: Names and interval/circuit settings for up to 30 custom presets you choose to keep in the formula picker. When you use the optional Wear OS companion, this library and the active formula can be sent to your paired watch as a read-only catalog.

This data is not transmitted to or shared with the developer. If you use the Wear OS companion, saved-preset names and timing settings, the active formula, and a small workout-completion record move between your paired devices through the Wear OS Data Layer. Workout history is stored in the phone app.

### Runtime Data

During app usage, the app temporarily stores timer state (current interval, time remaining, phase) in memory. This data is not persisted and is cleared when you close the app or reset the timer.

### Health and Fitness Data

The app stores workout completion data locally on your device, including:

- Dates when workouts were completed
- Number of workouts completed per day
- Total minutes walked per day
- Optional step counts and heart-rate summaries for completed workouts when Workout Metrics are enabled and Health Connect data is available

This health and fitness data is stored locally using Android's Room database and is not uploaded to developer-operated servers. A watch-completed workout sends its completion ID, formula name, duration, and completion time to the paired phone through the Wear OS Data Layer. You have full control over phone history, and it can be removed in the app or by uninstalling the app.

When you complete a workout, the app records the completion date, number of workouts for that day, total minutes walked, and optional Health Connect workout metrics if enabled and available. This data is stored locally in a database on your device and is used only to display your workout history, post-workout summaries, phase-aware heart-rate comparisons, and statistics within the app.

## Permissions

The app requests the following permissions:

- **VIBRATE**: Used to provide vibration notifications during interval phase changes. This permission does not collect or transmit any data.
- **POST_NOTIFICATIONS**: Required on Android 13+ to display system notifications. The app only shows timer-related notifications and does not collect data through notifications.
- **SCHEDULE_EXACT_ALARM**: Used for optional weekly goal reminders so Android can show them at the exact time you choose. This permission does not collect, store, transmit, or share any data.
- **WAKE_LOCK**: Used to keep the timer running accurately when your device screen is locked. This ensures the timer continues functioning in the background without collecting any data.
- **FOREGROUND_SERVICE**: Required by Android to run an active workout as a foreground service so interval timing can continue while the app is in the background.
- **FOREGROUND_SERVICE_HEALTH**: Required by Android for health/fitness foreground-service use cases on newer Android versions.
- **ACTIVITY_RECOGNITION**: Required by Android when starting a health foreground service on Android 14+.
  - This permission is used only to satisfy Android platform requirements for background workout execution.
- **Health Connect step and heart-rate read access**: Optional and off by default. Used only when Workout Metrics are enabled and permission is granted, so the app can read step counts and heart-rate samples for workout windows completed in Interval Walk Trainer and save summaries locally.
  - The app reads Health Connect data only; it does not write workouts or metrics back to Health Connect.
  - Steps are used for workout step summaries and trends. Heart rate is used for average/range summaries and fast-vs-slow interval phase comparisons.
  - If Health Connect is unavailable, permission is denied, or no samples exist for the workout window, Health Connect summaries are omitted.

## Third-Party Services

Interval Walk Trainer does not use any third-party analytics, advertising, or tracking services. The app does not integrate with social media platforms.

### System Services

The app uses the following Android system services, which are provided by your device's operating system:

- **Text-to-Speech (TTS)**: Used for optional voice notifications. TTS is a system service that runs locally on your device. We do not have access to any data processed by TTS.
- **Vibration Service**: Used for vibration notifications. This service operates locally on your device.
- **Alarm Manager**: Used for optional weekly goal reminders. Reminder settings remain local on your device.
- **Wear OS Data Layer**: When the optional watch companion is installed, Google Play services transfers the saved-preset catalog and active formula to the paired watch and returns completed-workout details to the phone. Delivery can be queued while devices are disconnected. The developer does not receive this data.

## Data Sharing

We do not sell workout data or send it to developer-operated servers. The optional Wear OS functionality transmits the limited pairing data described above through Google Play services solely to synchronize your paired devices.

## Data Security

Local preferences and workout history use Android's standard application storage mechanisms. Wear OS synchronization is limited to paired devices recognized by Google Play services, and stable random completion IDs prevent duplicate history entries.

## Children's Privacy

Interval Walk Trainer is intended for adults and is listed on Google Play for users age 18 and older. The app is not directed to children or minors, and we do not knowingly collect personal information from children. Because the app does not collect or transmit personal data, there is no child personal information for us to receive, store on our servers, sell, or share.

## Your Rights

Since we do not collect personal data, there is no data to access, modify, or delete from our servers. App preferences and workout history stored on your phone can be cleared by uninstalling the app. Workout history can also be deleted from within the app.

### Deleting Your Workout Data

You can delete all your workout history and statistics at any time directly from within the app. The app includes a "Clear All" feature in the Stats screen that allows you to permanently delete all workout records, including:

- All workout completion dates
- All workout counts and minutes tracked
- All statistics (streaks, totals, averages)

This action is permanent and cannot be undone. After clearing your data, you can continue using the app and new workouts will be tracked from that point forward.

## Changes to This Privacy Policy

We may update this Privacy Policy from time to time. Any changes will be posted on this page with an updated "Last Updated" date. We encourage you to review this Privacy Policy periodically.

## Contact Us

If you have any questions about this Privacy Policy, please contact us through the app's support channels or GitHub repository.

For support or privacy-related inquiries, please open an issue on the app's GitHub repository or contact the developer through the contact information provided in the Google Play Store listing.

## Compliance

This Privacy Policy complies with:

- Google Play Store requirements
- General Data Protection Regulation (GDPR)
- California Consumer Privacy Act (CCPA)
- Other applicable privacy laws

---

**Note**: This privacy policy reflects the phone app, optional Wear OS companion, Health Connect integration, and paired-device synchronization currently provided by Interval Walk Trainer.