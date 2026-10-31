# SmartHealthcare

SmartHealthcare is a full-stack healthcare management platform built with React, Spring Boot, and MySQL. It supports doctor appointments, telemedicine, lab tests, sample collection, prescription-based medicine ordering, live health monitoring, emergency alerts, and AI chatbot integration.

## Project Structure

```text
SmartHealthcare/
├── frontend/
├── backend/
├── database/
└── docs/
```

## Features

- Patient, Doctor, Admin, Laboratory, and Pharmacy roles
- JWT authentication and role-based backend protection
- Doctor, lab, and pharmacy approval workflows
- Patient smartwatch data and live vitals dashboard
- Emergency alert logic and WebSocket notifications
- Appointment booking with duplicate-booking prevention
- Lab test booking and report PDF upload
- Prescription PDF upload and medicine ordering
- Admin analytics and approval controls
- AI chatbot integration points
- Responsive healthcare interface with dark mode, charts, and telemedicine screen

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

Spring Boot does not automatically run `data.sql` by default. Set `SQL_INIT_MODE=always` only when you intentionally want Spring to execute it.

Seeded demo accounts use password `admin123`:

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

Swagger UI: `http://localhost:8081/swagger-ui/index.html`

## Frontend Setup

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Frontend URL: `http://localhost:3000`

## Environment Variables

Backend variables are listed in `backend/.env.example`, and frontend variables are listed in `frontend/.env.example`.

Configure your own credentials for:

- MySQL
- JWT secret
- OpenAI API
- Twilio
- SMTP
- Razorpay

Never commit real passwords, API keys, or other private credentials.

## Notes

Twilio, Firebase, Google Fit, Jitsi, and OpenAI integrations require valid vendor credentials and any additional production configuration before deployment.

Razorpay payment processing requires valid API keys configured through environment variables. Without credentials, the application may use its local demo payment fallback.
