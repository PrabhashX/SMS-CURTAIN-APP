# SMS Curtains V7 — Android phone-only billing and cloud sync

See **PHONE_CLOUD_SETUP.md** for the setup, installation and test process. The app uses the cloud API at `https://sms-curtain-app.onrender.com` and securely stores records via the Node.js backend connected to MongoDB Atlas.

Features: separate window and door measurements, conversion to feet, SMS Curtains 1 measured foot = 1 fabric metre rule, automatic eyelets/fusion/wood/brackets/endcaps, simple three-step bill creation, customer bills and payment history, admin/employee role permissions, admin approvals for employee payments/edits, offline Room storage and automatic sync, 58mm/80mm Bluetooth thermal print and Android Print/PDF.

Never embed MongoDB URI or database passwords in the APK. Keep backend secrets only in the Render environment.
