# SmartHealthcare

Full stack healthcare management web application with React, Spring Boot, MySQL, JWT auth, local PDF storage, WebSocket emergency alerts, OTP/email verification hooks, and AI chatbot integration points.

## Project Structure

```text
SmartHealthcare/
  frontend/
  backend/
  database/
  docs/
```

## Features

- Patient, Doctor, Admin, Laboratory, and Pharmacy roles
- JWT login and role-based backend protection
- Doctor/lab/pharmacy approval workflow
- Patient smartwatch data, live vitals dashboard, emergency alert logic
- Appointment slots with duplicate-booking prevention
- Lab test booking and report PDF upload
- Prescription PDF upload and medicine ordering
- Admin analytics and approval controls
- OpenAI-ready AI chatbot with disease-specific fallback guidance
- WebSocket topic for emergency alerts
- Responsive blue/white healthcare UI with glassmorphism, dark mode, cards, charts, tables, notification bell, and telemedicine screen

## Requirements

- Java 21
- Maven 3.9+
- Node.js 18+
- MySQL 8+

## Database Setup

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed.sql
```

Spring Boot does not auto-run `data.sql` by default, so an existing database will not be overwritten on every startup. Set `SQL_INIT_MODE=always` only when you intentionally want Spring to run `backend/src/main/resources/data.sql`.

All seeded users use password `admin123`.

Demo accounts:

- Admin: `9000000000`
- Patient: `9876543210`
- Doctor: `9876543211`
- Laboratory: `9876543212`
- Pharmacy: `9876543213`

## Backend Setup

```bash
cd backend
cp .env.example .env
mvn spring-boot:run
```

Swagger UI:

```text
http://localhost:8081/swagger-ui/index.html
```

## Frontend Setup

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Frontend URL:

```text
http://localhost:3000
```

## Environment Variables

Backend variables are listed in `backend/.env.example`; frontend variables are listed in `frontend/.env.example`.

For production, replace:

- `JWT_SECRET`
- MySQL credentials
- `OPENAI_API_KEY`
- Twilio credentials
- SMTP credentials. The default sender is `amangoyal682003@gmail.com`; set `MAIL_PASSWORD` to a Gmail App Password for approval/rejection emails to actually send.
- Razorpay credentials. Set `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` from Razorpay Dashboard API Keys. Without keys, the app uses a local demo payment fallback.

Razorpay Checkout supports UPI, cards, net banking, wallets, and other methods enabled in your Razorpay account. SmartHealthcare creates a Razorpay order on the backend, opens Checkout on the frontend, then verifies the payment signature on the backend before saving payment success.

## Notes

The Twilio, Firebase, Google Fit, Jitsi, and OpenAI pieces are implemented as integration-ready hooks or URLs. Real vendor credentials and SDK flows should be added before production deployment.
