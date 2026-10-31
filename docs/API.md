# SmartHealthcare REST API

Base URL: `http://localhost:8081/api`

Authentication: send `Authorization: Bearer <jwt>` after login.

## Auth

- `POST /auth/login` with `{ "mobileNumber": "...", "password": "..." }`
- `POST /auth/patient/register`
- `POST /auth/doctor/register`
- `POST /auth/laboratory/register`
- `POST /auth/pharmacy/register`
- `POST /auth/otp/verify?mobileNumber=...&otp=123456`
- `POST /auth/email/verify?email=...&code=123456`

## Patient

- `GET /patient/{id}/dashboard`
- `GET /patient/doctors/search?speciality=Cardiology&city=Mumbai`
- `POST /patient/appointments`
- `POST /patient/smartwatch/connect?patientId=1&provider=Google%20Fit`
- `POST /patient/smartwatch/data`
- `GET /patient/lab-tests`
- `POST /patient/lab-bookings?patientId=1&testId=1`
- `POST /patient/prescriptions/upload` multipart PDF

## Doctor

- `GET /doctor/{id}/dashboard`
- `POST /doctor/slots`
- `GET /doctor/{doctorId}/slots`
- `POST /doctor/prescriptions/upload` multipart PDF

## Laboratory

- `GET /labs/search?city=Mumbai`
- `POST /labs/{labId}/tests`
- `GET /labs/{labId}/tests`
- `GET /labs/bookings`
- `POST /labs/reports/upload` multipart PDF

## Pharmacy

- `POST /pharmacy/{pharmacyId}/medicines`
- `GET /pharmacy/{pharmacyId}/medicines`
- `POST /pharmacy/orders`
- `PATCH /pharmacy/orders/{id}/status?status=OUT_FOR_DELIVERY`

## Admin

- `GET /admin/dashboard`
- `GET /admin/approvals`
- `PATCH /admin/users/{id}/status?status=APPROVED`
- `GET /admin/users`

## AI Chatbot

- `POST /chatbot/ask`
- `GET /chatbot/history/{patientId}`

## WebSocket

STOMP endpoint: `/ws`

Subscribe to `/topic/emergency-alerts` for live patient emergency notifications.
