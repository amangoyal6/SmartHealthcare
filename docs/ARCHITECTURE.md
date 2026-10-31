# SmartHealthcare Architecture

SmartHealthcare is organized as a monorepo:

- `frontend/`: React, React Router, Axios, Chart.js, responsive glassmorphism UI.
- `backend/`: Spring Boot REST API, JWT security, JPA repositories, WebSocket alerts, local PDF storage.
- `database/`: MySQL schema and seed data.
- `docs/`: API and architecture notes.

## Main flows

1. A patient registers, verifies OTP, connects a smartwatch, and books appointments, lab tests, telemedicine, or pharmacy orders.
2. Doctors, labs, and pharmacies register with `PENDING` status. Admin approves or blocks accounts.
3. Appointment slots are unique per doctor and slot start time. Once booked, the slot is marked `booked`.
4. Smartwatch vitals are stored in `smartwatch_data`. Abnormal BP, pulse, or oxygen creates `emergency_alerts`, simulates SMS delivery, and broadcasts to `/topic/emergency-alerts`.
5. Prescriptions and reports are stored as local PDFs under `backend/uploads`.
6. The chatbot currently has safe disease-rule fallbacks and is ready for OpenAI API replacement through `OPENAI_API_KEY`.
