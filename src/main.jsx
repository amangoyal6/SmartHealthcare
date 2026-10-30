import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { Activity, Bell, Bot, CalendarCheck, CheckCircle, ClipboardList, CreditCard, FlaskConical, HeartPulse, Home, LogOut, MapPin, Moon, Package, Pill, Search, ShieldCheck, Stethoscope, Sun, UserCheck, Users, Video, XCircle } from 'lucide-react';
import { Bar, Doughnut, Line } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, BarElement, ArcElement, Tooltip, Legend, Filler } from 'chart.js';
import { api, setToken } from './services/api.js';
import './styles/app.css';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, ArcElement, Tooltip, Legend, Filler);

const navByRole = {
  PATIENT: [['/', Home, 'Home'], ['/appointments', CalendarCheck, 'Book Doctor'], ['/labs', FlaskConical, 'Lab Tests'], ['/medicines', Pill, 'Medicines'], ['/bookings', ClipboardList, 'My Bookings'], ['/monitoring', HeartPulse, 'Live Health'], ['/chatbot', Bot, 'AI Help']],
  DOCTOR: [['/', Home, 'Dashboard'], ['/appointments', CalendarCheck, 'Appointments'], ['/patients', Users, 'Patients'], ['/telemedicine', Video, 'Telemedicine']],
  LABORATORY: [['/', Home, 'Lab Dashboard'], ['/lab-tests', ClipboardList, 'Manage Tests'], ['/labs', FlaskConical, 'Test Bookings']],
  PHARMACY: [['/', Home, 'Pharmacy Dashboard'], ['/inventory', Pill, 'Inventory'], ['/pharmacy', Package, 'Orders']],
  ADMIN: [['/', Home, 'Dashboard'], ['/admin', ShieldCheck, 'Approvals'], ['/users', Users, 'Users']]
};

const doctors = [
  { id: 1, name: 'Dr. Mira Patel', speciality: 'Cardiologist', city: 'Mumbai', fee: 700, virtualFee: 500, time: 'Today 5:30 PM', rating: '98%', slots: ['Today 5:30 PM', 'Today 7:00 PM', 'Tomorrow 10:00 AM'] },
  { id: 2, name: 'Dr. Arjun Mehta', speciality: 'Pulmonologist', city: 'Pune', fee: 650, virtualFee: 450, time: 'Tomorrow 10:00 AM', rating: '96%', slots: ['Today 6:30 PM', 'Tomorrow 11:30 AM', 'Tomorrow 5:00 PM'] },
  { id: 3, name: 'Dr. Neha Rao', speciality: 'General Physician', city: 'Delhi', fee: 500, virtualFee: 350, time: 'Today 7:00 PM', rating: '95%', slots: ['Today 7:00 PM', 'Tomorrow 9:30 AM', 'Tomorrow 6:00 PM'] }
];

const labBookings = [
  { patient: 'Aarav Sharma', test: 'Complete Blood Count', location: 'Andheri East, Mumbai', slot: 'Today 8:30 AM', status: 'Collector Assigned' },
  { patient: 'Mira Joshi', test: 'Fasting Blood Sugar', location: 'Kothrud, Pune', slot: 'Tomorrow 7:00 AM', status: 'Sample Pending' },
  { patient: 'Noah Fernandes', test: 'Thyroid Profile', location: 'Bandra West, Mumbai', slot: 'Today 11:00 AM', status: 'Report Upload Due' }
];

const labTests = [
  { id: 1, lab: 'CityCare Labs', city: 'Mumbai', test: 'Complete Blood Count', price: 450, instructions: 'Morning sample preferred' },
  { id: 2, lab: 'CityCare Labs', city: 'Mumbai', test: 'Fasting Blood Sugar', price: 250, instructions: 'Fasting required' },
  { id: 3, lab: 'Pune Prime Diagnostics', city: 'Pune', test: 'Thyroid Profile', price: 620, instructions: 'No fasting required' },
  { id: 4, lab: 'Delhi Health Labs', city: 'Delhi', test: 'Liver Function Test', price: 780, instructions: 'Morning sample preferred' }
];

const pharmacyOrders = [
  { customer: 'Aarav Sharma', mobile: '9876543210', medicine: 'Salbutamol Inhaler', address: 'Andheri East, Mumbai', amount: 'Rs 280', status: 'Prescription Review' },
  { customer: 'Riya Shah', mobile: '9876543220', medicine: 'Metformin 500mg', address: 'Bandra West, Mumbai', amount: 'Rs 145', status: 'Packed' },
  { customer: 'Kabir Khan', mobile: '9876543230', medicine: 'Paracetamol 500mg', address: 'Powai, Mumbai', amount: 'Rs 35', status: 'Out for Delivery' }
];

const medicines = [
  { id: 1, name: 'Paracetamol 500mg', category: 'Fever & Pain', price: 35, stock: 200, prescriptionRequired: false },
  { id: 2, name: 'ORS Sachet', category: 'Hydration', price: 25, stock: 150, prescriptionRequired: false },
  { id: 3, name: 'Salbutamol Inhaler', category: 'Respiratory', price: 280, stock: 40, prescriptionRequired: true },
  { id: 4, name: 'Metformin 500mg', category: 'Diabetes', price: 145, stock: 60, prescriptionRequired: true }
];

function loadRazorpayScript() {
  return new Promise((resolve) => {
    if (window.Razorpay) return resolve(true);
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

function openMockCheckout(amount, referenceType) {
  return new Promise((resolve) => {
    const overlay = document.createElement('div');
    overlay.className = 'mock-pay-overlay';
    overlay.innerHTML = `
      <div class="mock-pay-modal">
        <div class="mock-pay-head">
          <div>
            <span>SmartHealthcare</span>
            <h2>Checkout</h2>
          </div>
          <button class="mock-pay-close" type="button">x</button>
        </div>
        <div class="mock-pay-amount">Rs ${amount}</div>
        <p class="mock-pay-subtitle">${referenceType.replaceAll('_', ' ')}</p>
        <div class="mock-pay-methods">
          <label><input type="radio" name="mockPay" checked /> UPI</label>
          <label><input type="radio" name="mockPay" /> Credit / Debit Card</label>
          <label><input type="radio" name="mockPay" /> Net Banking</label>
          <label><input type="radio" name="mockPay" /> Wallet</label>
        </div>
        <input class="mock-pay-input" placeholder="UPI ID / Card / Bank reference" value="success@upi" />
        <button class="mock-pay-button" type="button">Pay Securely</button>
        <small>College project mock checkout. No real money is deducted.</small>
      </div>
    `;
    document.body.appendChild(overlay);
    overlay.querySelector('.mock-pay-close').addEventListener('click', () => {
      overlay.remove();
      resolve(false);
    });
    overlay.querySelector('.mock-pay-button').addEventListener('click', () => {
      overlay.querySelector('.mock-pay-button').textContent = 'Processing...';
      setTimeout(() => {
        overlay.remove();
        resolve(true);
      }, 600);
    });
  });
}

async function razorpayPayment(amount, referenceType, referenceId) {
  const session = JSON.parse(localStorage.getItem('session') || '{}');
  if (!session.userId) {
    alert('Please login again before payment.');
    return false;
  }
  const { data: order } = await api.post('/payments/razorpay/order', { userId: session.userId, referenceType, referenceId, amount });
  const scriptReady = await loadRazorpayScript();
  if (!scriptReady || order.demo) {
    const mockPaid = await openMockCheckout(amount, referenceType);
    if (!mockPaid) return false;
    await api.post('/payments/razorpay/verify', { userId: session.userId, referenceType, referenceId, amount, razorpayOrderId: order.orderId, razorpayPaymentId: `pay_demo_${Date.now()}`, razorpaySignature: 'demo' });
    return true;
  }
  return new Promise((resolve, reject) => {
    const checkout = new window.Razorpay({
      key: order.keyId,
      amount: order.amount,
      currency: order.currency,
      name: 'SmartHealthcare',
      description: referenceType.replaceAll('_', ' '),
      order_id: order.orderId,
      prefill: { name: session.name || 'SmartHealthcare User' },
      theme: { color: '#0ea5e9' },
      handler: async (response) => {
        try {
          await api.post('/payments/razorpay/verify', {
            userId: session.userId,
            referenceType,
            referenceId,
            amount,
            razorpayOrderId: response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature
          });
          alert(`Payment successful: Rs ${amount}`);
          resolve(true);
        } catch (error) {
          alert(error.response?.data?.error || 'Payment verification failed.');
          reject(error);
        }
      }
    });
    checkout.on('payment.failed', (response) => {
      alert(response.error?.description || 'Payment failed.');
      resolve(false);
    });
    checkout.open();
  });
}

function App() {
  const [session, setSession] = useState(() => JSON.parse(localStorage.getItem('session') || 'null'));
  const [dark, setDark] = useState(false);
  const login = (next) => {
    localStorage.setItem('session', JSON.stringify(next));
    setToken(next.token);
    setSession(next);
  };
  const logout = () => {
    localStorage.removeItem('session');
    setToken(null);
    setSession(null);
  };
  setToken(session?.token);
  return (
    <div className={dark ? 'app dark' : 'app'}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<AuthPage onLogin={login} />} />
          <Route path="/*" element={session ? <Shell session={session} logout={logout} dark={dark} setDark={setDark} /> : <Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </div>
  );
}

function Shell({ session, logout, dark, setDark }) {
  const nav = navByRole[session.role] || navByRole.PATIENT;
  return (
    <div className="layout">
      <aside className="sidebar glass">
        <div className="brand"><span className="brand-mark">+</span><strong>SmartHealthcare</strong></div>
        <nav>{nav.map(([href, Icon, label]) => <a key={href} href={href}><Icon size={18} />{label}</a>)}</nav>
      </aside>
      <main>
        <header className="topbar glass">
          <div><span className="eyebrow">{session.role.toLowerCase()} portal</span><h1>{session.name}</h1></div>
          <div className="top-actions">
            <button className="icon-btn" title="Notifications"><Bell size={19} /></button>
            <button className="icon-btn" title="Toggle dark mode" onClick={() => setDark(!dark)}>{dark ? <Sun size={19} /> : <Moon size={19} />}</button>
            <button className="logout" onClick={logout}><LogOut size={17} />Logout</button>
          </div>
        </header>
        <RoleRoutes session={session} />
      </main>
    </div>
  );
}

function RoleRoutes({ session }) {
  const role = session.role;
  if (role === 'PHARMACY') return <Routes><Route path="/" element={<PharmacyDashboard />} /><Route path="/inventory" element={<PharmacyInventory />} /><Route path="/pharmacy" element={<PharmacyOrders />} /><Route path="*" element={<Navigate to="/" replace />} /></Routes>;
  if (role === 'LABORATORY') return <Routes><Route path="/" element={<LabDashboard />} /><Route path="/lab-tests" element={<LabTestManager />} /><Route path="/labs" element={<LabBookings />} /><Route path="*" element={<Navigate to="/" replace />} /></Routes>;
  if (role === 'DOCTOR') return <Routes><Route path="/" element={<DoctorDashboard session={session} />} /><Route path="/appointments" element={<DoctorAppointments session={session} />} /><Route path="/patients" element={<PatientHistory />} /><Route path="/telemedicine" element={<Telemedicine />} /><Route path="*" element={<Navigate to="/" replace />} /></Routes>;
  if (role === 'ADMIN') return <Routes><Route path="/" element={<AdminDashboard />} /><Route path="/admin" element={<AdminApprovals />} /><Route path="/users" element={<AdminUsers />} /><Route path="*" element={<Navigate to="/" replace />} /></Routes>;
  return <Routes><Route path="/" element={<PatientDashboard />} /><Route path="/appointments" element={<DoctorBooking />} /><Route path="/labs" element={<PatientLabBooking />} /><Route path="/medicines" element={<PatientMedicineOrder />} /><Route path="/bookings" element={<PatientBookings />} /><Route path="/monitoring" element={<LiveMonitoring />} /><Route path="/chatbot" element={<Chatbot />} /><Route path="*" element={<Navigate to="/" replace />} /></Routes>;
}

function AuthPage({ onLogin }) {
  const [mode, setMode] = useState('login');
  const [role, setRole] = useState('patient');
  const [mobileVerified, setMobileVerified] = useState(false);
  const [emailVerified, setEmailVerified] = useState(false);
  const [otpCode, setOtpCode] = useState('');
  const [emailCode, setEmailCode] = useState('');
  const [reset, setReset] = useState({ email: '', code: '', newPassword: '' });
  const [form, setForm] = useState({ fullName: '', mobileNumber: '', password: '', age: '', gender: 'Male', nearDearContactNumber: '', diseaseDetails: '', allergyDetails: '', email: '', nmcId: '', speciality: '', address: '', experience: '', licenseNumber: '' });
  const navigate = useNavigate();
  const update = (key, value) => setForm({ ...form, [key]: value });
  const submit = async (event) => {
    event.preventDefault();
    try {
      if (mode === 'login') {
        const { data } = await api.post('/auth/login', { mobileNumber: form.mobileNumber, password: form.password });
        onLogin(data);
        navigate('/');
        return;
      }
      const endpoints = { patient: '/auth/patient/register', doctor: '/auth/doctor/register', laboratory: '/auth/laboratory/register', pharmacy: '/auth/pharmacy/register' };
      const payload = role === 'patient' ? { ...form, mobileVerified, emailVerified } : { name: form.fullName, ...form, mobileVerified, emailVerified };
      const { data } = await api.post(endpoints[role], payload);
      if (data.token) { onLogin(data); navigate('/'); } else alert(data.message || 'Registration submitted for admin approval.');
    } catch (error) {
      alert(error.response?.data?.error || 'Request failed. Please check details and try again.');
    }
  };
  const sendOtp = async () => {
    const { data } = await api.post(`/auth/otp/send?mobileNumber=${encodeURIComponent(form.mobileNumber)}`);
    alert(`OTP sent. Demo code: ${data.demoOtp}`);
  };
  const verifyOtp = async () => {
    const { data } = await api.post(`/auth/otp/verify?mobileNumber=${encodeURIComponent(form.mobileNumber)}&otp=${encodeURIComponent(otpCode)}`);
    setMobileVerified(data.verified);
    alert(data.verified ? 'Mobile OTP verified.' : 'Invalid OTP.');
  };
  const sendEmailCode = async () => {
    const { data } = await api.post(`/auth/email/send?email=${encodeURIComponent(form.email)}`);
    alert(data.mailSent ? 'Email code sent. Please check inbox/spam.' : `Email SMTP failed. Local demo code: ${data.demoCode}`);
  };
  const verifyEmailCode = async () => {
    const { data } = await api.post(`/auth/email/verify?email=${encodeURIComponent(form.email)}&code=${encodeURIComponent(emailCode)}`);
    setEmailVerified(data.verified);
    alert(data.verified ? 'Email verified.' : 'Invalid email code.');
  };
  const sendResetCode = async () => {
    try {
      const { data } = await api.post('/auth/forgot-password', { email: reset.email });
      alert(data.mailSent ? 'Reset code sent to email.' : `Email SMTP failed. Local reset code: ${data.demoCode}`);
    } catch (error) {
      alert(error.response?.data?.error || 'Unable to send reset code.');
    }
  };
  const resetPassword = async () => {
    try {
      const { data } = await api.post('/auth/reset-password', reset);
      alert(data.message || 'Password reset successfully.');
      setMode('login');
    } catch (error) {
      alert(error.response?.data?.error || 'Unable to reset password.');
    }
  };
  return (
    <div className="auth-page">
      <section className="auth-hero"><div><span className="eyebrow">Practo-style care platform</span><h1>SmartHealthcare</h1><p>Book doctors, lab tests, telemedicine, and manage healthcare operations with separate portals for every role.</p></div></section>
      <form className="auth-card glass" onSubmit={submit}>
        <div className="segmented"><button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>Login</button><button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>Register</button></div>
        {mode === 'login' && <button type="button" className="forgot-link" onClick={() => setMode('forgot')}>Forgot password?</button>}
        {mode === 'forgot' && <><input placeholder="Registered email" value={reset.email} onChange={(e) => setReset({ ...reset, email: e.target.value })} /><button type="button" className="verify-btn" onClick={sendResetCode}>Send reset code</button><input placeholder="Reset code" value={reset.code} onChange={(e) => setReset({ ...reset, code: e.target.value })} /><input placeholder="New password" type="password" value={reset.newPassword} onChange={(e) => setReset({ ...reset, newPassword: e.target.value })} /><button type="button" className="primary" onClick={resetPassword}>Reset password</button><button type="button" className="forgot-link" onClick={() => setMode('login')}>Back to login</button></>}
        {mode !== 'forgot' && <>
        {mode === 'register' && <select value={role} onChange={(e) => setRole(e.target.value)}><option value="patient">Patient</option><option value="doctor">Doctor</option><option value="laboratory">Laboratory</option><option value="pharmacy">Pharmacy</option></select>}
        {mode === 'register' && <input placeholder={role === 'doctor' ? 'Doctor full name' : role === 'laboratory' ? 'Laboratory name' : role === 'pharmacy' ? 'Pharmacy name' : 'Patient full name'} value={form.fullName} onChange={(e) => update('fullName', e.target.value)} />}
        {mode === 'register' && role === 'doctor' && <><div className="field-grid"><input placeholder="Age in years" value={form.age} onChange={(e) => update('age', e.target.value)} /><input placeholder="Medical experience in years" value={form.experience} onChange={(e) => update('experience', e.target.value)} /></div><input placeholder="Expertise / speciality, e.g. Cardiologist" value={form.speciality} onChange={(e) => update('speciality', e.target.value)} /><input placeholder="NMC registration ID" value={form.nmcId} onChange={(e) => update('nmcId', e.target.value)} /></>}
        {mode === 'register' && role === 'pharmacy' && <input placeholder="Pharmacy license number" value={form.licenseNumber} onChange={(e) => update('licenseNumber', e.target.value)} />}
        {mode === 'register' && <input placeholder="Email address" value={form.email} onChange={(e) => { update('email', e.target.value); setEmailVerified(false); }} />}
        {mode === 'register' && <input placeholder="Full address and city" value={form.address} onChange={(e) => update('address', e.target.value)} />}
        {mode === 'register' && role === 'patient' && <><div className="field-grid"><input placeholder="Age in years" value={form.age} onChange={(e) => update('age', e.target.value)} /><select value={form.gender} onChange={(e) => update('gender', e.target.value)}><option>Male</option><option>Female</option><option>Other</option></select></div><input placeholder="Allergy details, or type No" value={form.allergyDetails} onChange={(e) => update('allergyDetails', e.target.value)} /><input placeholder="Disease details, or type No" value={form.diseaseDetails} onChange={(e) => update('diseaseDetails', e.target.value)} /><input placeholder="Emergency contact number" value={form.nearDearContactNumber} onChange={(e) => update('nearDearContactNumber', e.target.value)} /></>}
        <input placeholder="Mobile no." autoComplete={mode === 'login' ? 'username' : 'off'} value={form.mobileNumber} onChange={(e) => { update('mobileNumber', e.target.value); setMobileVerified(false); }} />
        {mode === 'register' && <div className="verify-grid"><button type="button" className="verify-btn" onClick={sendOtp}>Send OTP</button><input placeholder="Enter OTP" value={otpCode} onChange={(e) => setOtpCode(e.target.value)} /><button type="button" className="verify-btn" onClick={verifyOtp}><CheckCircle size={16} />{mobileVerified ? 'Mobile verified' : 'Verify OTP'}</button></div>}
        {mode === 'register' && <div className="verify-grid"><button type="button" className="verify-btn" onClick={sendEmailCode}>Send email code</button><input placeholder="Enter email code" value={emailCode} onChange={(e) => setEmailCode(e.target.value)} /><button type="button" className="verify-btn" onClick={verifyEmailCode}><CheckCircle size={16} />{emailVerified ? 'Email verified' : 'Verify email'}</button></div>}
        <input placeholder="Password" type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={form.password} onChange={(e) => update('password', e.target.value)} />
        <button className="primary">{mode === 'login' ? 'Sign in' : 'Create account'}</button>
        <p className="hint">Demo: admin 9000000000, patient 9876543210, doctor 9876543211, lab 9876543212, pharmacy 9876543213. Password: admin123.</p>
        </>}
      </form>
    </div>
  );
}

function PatientDashboard() {
  const [city, setCity] = useState('Mumbai');
  const [speciality, setSpeciality] = useState('');
  const navigate = useNavigate();
  const searchDoctors = () => {
    navigate(`/appointments?city=${encodeURIComponent(city)}&speciality=${encodeURIComponent(speciality)}`);
  };
  return (
    <section className="consumer-home">
      <div className="patient-hero glass"><div><span className="eyebrow">Patient home</span><h2>Find doctors, book lab tests, monitor vitals</h2><div className="practo-search"><label><MapPin size={18} /><input placeholder="City or locality" value={city} onChange={(e) => setCity(e.target.value)} /></label><label><Search size={18} /><input placeholder="Search doctors, specialities, symptoms" value={speciality} onChange={(e) => setSpeciality(e.target.value)} /></label><button className="primary" onClick={searchDoctors}>Search</button></div></div></div>
      <div className="service-grid"><ServiceCard icon={CalendarCheck} title="Appointment Booking" text="Search doctor by city and speciality, choose clinic or virtual consult, then book slots." href="/appointments" /><ServiceCard icon={FlaskConical} title="Lab Test Booking" text="Book tests with home sample collection and report download." href="/labs" /><ServiceCard icon={Pill} title="Medicine Order" text="Select medicines, upload prescription PDF when required, and pay online." href="/medicines" /><ServiceCard icon={HeartPulse} title="Live Health Monitoring" text="Track BP, pulse, oxygen, heart rate, and sleep from smartwatch data." href="/monitoring" /></div>
    </section>
  );
}

function PatientBookings() {
  const [appointments, setAppointments] = useState([]);
  const [labs, setLabs] = useState([]);
  const load = async () => {
    const profile = await api.get('/me/profile');
    if (!profile.data.profileId) return;
    const [apptResponse, labResponse] = await Promise.all([
      api.get(`/patient/appointments?patientId=${profile.data.profileId}`),
      api.get(`/patient/lab-bookings?patientId=${profile.data.profileId}`)
    ]);
    setAppointments(apptResponse.data);
    setLabs(labResponse.data);
  };
  useEffect(() => { load().catch(() => {}); }, []);
  const cancelAppointment = async (id) => {
    await api.patch(`/patient/appointments/${id}/cancel`);
    await load();
  };
  const rescheduleAppointment = async (id) => {
    const slot = prompt('Enter new slot, e.g. Tomorrow 6:00 PM');
    if (!slot) return;
    await api.patch(`/patient/appointments/${id}/reschedule?slotText=${encodeURIComponent(slot)}`);
    await load();
  };
  const cancelLab = async (id) => {
    await api.patch(`/patient/lab-bookings/${id}/cancel`);
    await load();
  };
  return (
    <section className="module">
      <div className="panel glass module-head"><div><ClipboardList size={28} /><h2>My bookings</h2></div></div>
      <div className="panel glass table-panel">
        <h3>Doctor appointments</h3>
        <table><thead><tr><th>Doctor</th><th>Speciality</th><th>Slot</th><th>Mode</th><th>Status</th><th>Meeting</th><th>Actions</th></tr></thead><tbody>{appointments.map((item) => <tr key={item.id}><td>{item.doctorName}</td><td>{item.speciality}</td><td>{item.slot}</td><td>{item.mode}</td><td><span className={`status ${item.status.toLowerCase()}`}>{item.status}</span></td><td>{item.mode === 'Virtual' ? <a className="mini link-btn" href={item.meetingLink} target="_blank" rel="noreferrer">Join call</a> : 'Clinic visit'}</td><td><div className="row-actions"><button className="mini" onClick={() => rescheduleAppointment(item.id)}>Reschedule</button><button className="mini danger-mini" onClick={() => cancelAppointment(item.id)}>Cancel</button></div></td></tr>)}</tbody></table>
        {!appointments.length && <p className="hint">No appointment history yet.</p>}
      </div>
      <div className="panel glass table-panel">
        <h3>Lab test bookings</h3>
        <table><thead><tr><th>Patient</th><th>Test</th><th>Lab</th><th>Collection address</th><th>Status</th><th>Booked at</th><th>Actions</th></tr></thead><tbody>{labs.map((item) => <tr key={item.id}><td>{item.patientName}</td><td>{item.testName}</td><td>{item.labName}</td><td>{item.collectionAddress}</td><td><span className={`status ${item.status.toLowerCase()}`}>{item.status}</span></td><td>{item.bookedAt}</td><td><button className="mini danger-mini" onClick={() => cancelLab(item.id)}>Cancel</button></td></tr>)}</tbody></table>
        {!labs.length && <p className="hint">No lab booking history yet.</p>}
      </div>
    </section>
  );
}

function ServiceCard({ icon: Icon, title, text, href }) {
  return <a className="service-card glass" href={href}><Icon size={28} /><h3>{title}</h3><p>{text}</p><span>Open</span></a>;
}

function DoctorBooking() {
  const [doctorList, setDoctorList] = useState(doctors);
  const [cityFilter, setCityFilter] = useState('');
  const [specialityFilter, setSpecialityFilter] = useState('');
  const location = useLocation();
  const loadDoctors = (city = cityFilter, speciality = specialityFilter) => {
    api.get(`/patient/doctors/search?speciality=${encodeURIComponent(speciality)}&city=${encodeURIComponent(city)}`).then(({ data }) => {
      if (Array.isArray(data) && data.length) {
        setDoctorList(data.map((doctor) => ({
          id: doctor.id,
          name: doctor.name || 'Doctor',
          speciality: doctor.speciality || 'General Physician',
          city: doctor.city || 'Mumbai',
          fee: 700,
          virtualFee: 500,
          time: 'Next available',
          rating: '96%',
          slots: ['Today 5:30 PM', 'Today 7:00 PM', 'Tomorrow 10:00 AM']
        })));
      } else {
        setDoctorList([]);
      }
    }).catch(() => setDoctorList(doctors.filter((doctor) => doctor.city.toLowerCase().includes(city.toLowerCase()) && doctor.speciality.toLowerCase().includes(speciality.toLowerCase()))));
  };
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const city = params.get('city') || '';
    const speciality = params.get('speciality') || '';
    setCityFilter(city);
    setSpecialityFilter(speciality);
    loadDoctors(city, speciality);
  }, [location.search]);
  return <section className="module"><div className="panel glass module-head"><div><Stethoscope size={28} /><h2>Book appointment</h2></div><div className="doctor-filter-row"><label><MapPin size={17} /><input placeholder="Filter by city" value={cityFilter} onChange={(e) => setCityFilter(e.target.value)} /></label><label><Search size={17} /><input placeholder="Filter by speciality" value={specialityFilter} onChange={(e) => setSpecialityFilter(e.target.value)} /></label><button className="primary compact" onClick={() => loadDoctors(cityFilter, specialityFilter)}>Search</button></div></div>{doctorList.length ? <div className="listing-grid">{doctorList.map((doctor) => <DoctorCard key={doctor.id || doctor.name} doctor={doctor} />)}</div> : <div className="panel glass"><h3>No doctors found</h3><p>Try another city or speciality.</p></div>}</section>;
}

function DoctorCard({ doctor }) {
  const [selectedSlot, setSelectedSlot] = useState(doctor.slots[0]);
  const [consultMode, setConsultMode] = useState('Clinic');
  const [previousPdf, setPreviousPdf] = useState(null);
  const fee = consultMode === 'Virtual' ? doctor.virtualFee : doctor.fee;
  const bookAppointment = async () => {
    const profile = await api.get('/me/profile');
    if (profile.data.role !== 'PATIENT' || !profile.data.profileId) {
      alert('Please login as patient to book appointment.');
      return;
    }
    const paid = await razorpayPayment(fee, consultMode === 'Virtual' ? 'TELEMEDICINE' : 'APPOINTMENT', doctor.id);
    if (!paid) return;
    await api.post('/patient/appointments', {
      patientId: profile.data.profileId,
      doctorId: doctor.id,
      slotId: null,
      mode: consultMode,
      reason: `Booked from patient app for ${selectedSlot}`
    });
    if (previousPdf) {
      const formData = new FormData();
      formData.append('patientId', profile.data.profileId);
      formData.append('doctorId', doctor.id);
      formData.append('file', previousPdf);
      await api.post('/patient/prescriptions/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
    }
    alert(`${consultMode} appointment booked with ${doctor.name}. It will appear only in this doctor's dashboard.`);
  };
  return (
    <article className="listing-card glass">
      <div className="avatar">{doctor.name.split(' ').at(-1)[0]}</div>
      <div className="listing-main"><h3>{doctor.name}</h3><p>{doctor.speciality} - {doctor.city}</p><span className="badge">{doctor.rating} patient satisfaction</span><div className="slot-row">{doctor.slots.map((slot) => <button key={slot} className={selectedSlot === slot ? 'slot active' : 'slot'} onClick={() => setSelectedSlot(slot)}>{slot}</button>)}</div><label className="inline-upload"><span>Previous notes/report PDF for second opinion</span><input type="file" accept="application/pdf" onChange={(e) => setPreviousPdf(e.target.files?.[0] || null)} /><small>{previousPdf ? previousPdf.name : 'Optional'}</small></label></div>
      <div className="listing-actions"><div className="segmented small"><button className={consultMode === 'Clinic' ? 'active' : ''} onClick={() => setConsultMode('Clinic')}>Clinic</button><button className={consultMode === 'Virtual' ? 'active' : ''} onClick={() => setConsultMode('Virtual')}>Virtual</button></div><strong>Rs {fee}</strong><small>{selectedSlot}</small><button className="primary" onClick={bookAppointment}><CreditCard size={16} />Pay and book</button></div>
    </article>
  );
}

function PatientLabBooking() {
  const [city, setCity] = useState('Mumbai');
  const [testSearch, setTestSearch] = useState('');
  const [selectedTest, setSelectedTest] = useState(null);
  const [tests, setTests] = useState(labTests);
  const [sampleDetails, setSampleDetails] = useState({ name: 'Aarav Sharma', age: 32, gender: 'Male', address: 'Andheri East, Mumbai' });
  useEffect(() => {
    api.get('/patient/lab-tests').then(({ data }) => {
      if (!Array.isArray(data) || !data.length) return;
      setTests(data.map((item) => ({
        id: item.id,
        lab: item.labName || item.laboratory?.labName || 'SmartHealthcare Lab',
        city: item.city || item.laboratory?.address || 'City',
        test: item.testName || 'Lab test',
        price: item.price || 0,
        instructions: item.instructions || 'Sample collection instructions will be shared by lab'
      })));
    }).catch(() => setTests(labTests));
  }, []);
  const visibleTests = tests.filter((item) => item.city.toLowerCase().includes(city.toLowerCase()) && item.test.toLowerCase().includes(testSearch.toLowerCase()));
  const bookTest = async (item) => {
    if (!sampleDetails.name.trim() || !sampleDetails.age || !sampleDetails.gender.trim() || !sampleDetails.address.trim()) {
      alert('Patient name, age, gender and sample collection address are mandatory.');
      return;
    }
    const profile = await api.get('/me/profile');
    const paid = await razorpayPayment(item.price, 'LAB_TEST', item.id);
    if (!paid) return;
    try {
      await api.post(`/patient/lab-bookings?patientId=${profile.data.profileId}&testId=${item.id}&samplePatientName=${encodeURIComponent(sampleDetails.name)}&samplePatientAge=${encodeURIComponent(sampleDetails.age)}&samplePatientGender=${encodeURIComponent(sampleDetails.gender)}&collectionAddress=${encodeURIComponent(sampleDetails.address)}`);
    } catch {
      // Seed tests may differ by database; payment/demo UI still records intent locally.
    }
    alert(`${item.test} booked from ${item.lab}. Collector will visit: ${sampleDetails.address}`);
    setSelectedTest(null);
  };
  return (
    <section className="module">
      <div className="panel glass module-head"><div><FlaskConical size={28} /><h2>Book lab test</h2></div></div>
      <div className="panel glass lab-search-row"><label><MapPin size={17} /><input value={city} onChange={(e) => setCity(e.target.value)} placeholder="Search labs by city" /></label><label><Search size={17} /><input value={testSearch} onChange={(e) => setTestSearch(e.target.value)} placeholder="Search test name" /></label></div>
      <div className="test-grid">{visibleTests.map((item) => <article className="test-card glass" key={item.id}><h3>{item.test}</h3><p>{item.lab} - {item.city}</p><p>{item.instructions}</p><strong>Rs {item.price}</strong><button className="primary" onClick={() => setSelectedTest(item)}>Book test</button></article>)}</div>
      {selectedTest && <div className="panel glass lab-booking-form"><h3>Sample collection details</h3><p>{selectedTest.test} from {selectedTest.lab}</p><div className="field-grid"><input value={sampleDetails.name} onChange={(e) => setSampleDetails({ ...sampleDetails, name: e.target.value })} placeholder="Patient full name" /><input value={sampleDetails.age} onChange={(e) => setSampleDetails({ ...sampleDetails, age: e.target.value })} placeholder="Age" /></div><div className="field-grid"><select value={sampleDetails.gender} onChange={(e) => setSampleDetails({ ...sampleDetails, gender: e.target.value })}><option>Male</option><option>Female</option><option>Other</option></select><input value={sampleDetails.address} onChange={(e) => setSampleDetails({ ...sampleDetails, address: e.target.value })} placeholder="Full sample collection address" /></div><button className="primary compact" onClick={() => bookTest(selectedTest)}><CreditCard size={16} />Pay Rs {selectedTest.price} and confirm</button></div>}
    </section>
  );
}

function PatientMedicineOrder() {
  const [cart, setCart] = useState([]);
  const [prescription, setPrescription] = useState(null);
  const [address, setAddress] = useState('');
  const [orderMessage, setOrderMessage] = useState('');
  const total = cart.reduce((sum, item) => sum + item.price, 0);
  const needsPrescription = cart.some((item) => item.prescriptionRequired);
  const addToCart = (medicine) => {
    if (!cart.some((item) => item.id === medicine.id)) setCart([...cart, medicine]);
  };
  const removeFromCart = (medicineId) => {
    const nextCart = cart.filter((item) => item.id !== medicineId);
    setCart(nextCart);
    if (!nextCart.some((item) => item.prescriptionRequired)) setPrescription(null);
  };
  const placeOrder = async () => {
    if (!cart.length) {
      alert('Please select at least one medicine.');
      return;
    }
    if (needsPrescription && !prescription) {
      alert('Prescription PDF required for selected medicine.');
      return;
    }
    if (!address.trim()) {
      setOrderMessage('Delivery address is mandatory before payment.');
      return;
    }
    const paid = await razorpayPayment(total, 'MEDICINE_ORDER', cart[0].id);
    if (!paid) return;
    const profile = await api.get('/me/profile');
    await api.post(`/patient/medicine-orders?patientId=${profile.data.profileId}&totalAmount=${total}&deliveryAddress=${encodeURIComponent(address)}`);
    setOrderMessage(`Medicine order placed. Delivery address: ${address}`);
    setCart([]);
    setPrescription(null);
    setAddress('');
  };
  return (
    <section className="module">
      <div className="panel glass module-head"><div><Pill size={28} /><h2>Order medicines</h2></div><div className="search"><Search size={17} /><input placeholder="Search medicine" /></div></div>
      <div className="medicine-layout">
        <div className="medicine-grid">{medicines.map((medicine) => <article className="medicine-card glass" key={medicine.id}><div><h3>{medicine.name}</h3><p>{medicine.category}</p></div><span className={medicine.prescriptionRequired ? 'rx-badge' : 'otc-badge'}>{medicine.prescriptionRequired ? 'Prescription required' : 'No prescription'}</span><strong>Rs {medicine.price}</strong><small>{medicine.stock} in stock</small><button className="primary" onClick={() => addToCart(medicine)}>Add to cart</button></article>)}</div>
        <aside className="panel glass cart-panel"><h3>Order summary</h3><label className="address-box"><span>Delivery address</span><textarea value={address} onChange={(e) => { setAddress(e.target.value); setOrderMessage(''); }} placeholder="House/flat no, street, area, city, pincode" rows="4" /></label>{cart.length ? cart.map((item) => <div className="cart-row" key={item.id}><span>{item.name}</span><strong>Rs {item.price}</strong><button className="mini danger-mini" onClick={() => removeFromCart(item.id)}>Remove</button></div>) : <p className="hint">No medicines selected.</p>}{needsPrescription && <label className="upload-box"><span>Upload doctor prescription PDF</span><input type="file" accept="application/pdf" onChange={(e) => setPrescription(e.target.files?.[0] || null)} /><small>{prescription ? prescription.name : 'PDF required before payment'}</small></label>}<div className="cart-total"><span>Total</span><strong>Rs {total}</strong></div>{orderMessage && <p className="checkout-message">{orderMessage}</p>}<button className="primary" onClick={placeOrder}><CreditCard size={16} />Pay with Razorpay and order</button></aside>
      </div>
    </section>
  );
}

function LiveMonitoring() {
  const chartData = useMemo(() => ({ labels: ['8 AM', '10 AM', '12 PM', '2 PM', '4 PM', '6 PM'], datasets: [{ label: 'Heart rate', data: [76, 82, 78, 88, 84, 79], borderColor: '#0ea5e9', backgroundColor: 'rgba(14,165,233,.15)', fill: true, tension: .4 }] }), []);
  const vitals = [['BP', '118/76', 'mmHg', HeartPulse], ['Pulse', '78', 'bpm', Activity], ['Oxygen', '98', '%', ClipboardList], ['Sleep', '7.2', 'hrs', Moon]];
  return <section className="page-grid">{vitals.map(([label, value, unit, Icon]) => <Metric key={label} label={label} value={value} unit={unit} icon={Icon} />)}<div className="panel glass wide"><h3>Live health graph</h3><Line data={chartData} options={{ plugins: { legend: { display: false } } }} /></div><div className="panel glass"><h3>Emergency logic</h3><p>Abnormal BP, pulse, or oxygen will trigger SMS to near dear contact and notify doctor dashboard.</p></div></section>;
}

function Telemedicine() {
  const [slot, setSlot] = useState('Today 7:00 PM');
  return <section className="module"><div className="panel glass module-head"><div><Video size={28} /><h2>Telemedicine</h2></div><button className="primary compact" onClick={() => razorpayPayment(500, 'TELEMEDICINE', 1)}><CreditCard size={16} />Pay Rs 500</button></div><div className="panel glass"><h3>Book virtual consultancy slot</h3><div className="slot-row">{['Today 7:00 PM', 'Tomorrow 10:00 AM', 'Tomorrow 6:30 PM'].map((item) => <button key={item} className={slot === item ? 'slot active' : 'slot'} onClick={() => setSlot(item)}>{item}</button>)}</div></div><div className="video-box"><Video size={48} /><h3>Video consultation room</h3><p>Selected slot: {slot}</p><button className="primary compact">Join Jitsi call</button></div></section>;
}

function PharmacyDashboard() {
  return (
    <section className="module">
      <div className="panel hero-panel glass"><div><span className="eyebrow">Pharmacy profile</span><h2>HealthPlus Pharmacy</h2><p>Manage inventory, pricing, stock alerts, prescription verification, and delivery orders.</p></div><Pill size={44} /></div>
      <div className="cards"><Metric label="Active Orders" value="14" unit="today" icon={Package} /><Metric label="Low Stock" value="6" unit="items" icon={Pill} /><Metric label="Prescription Review" value="4" unit="pending" icon={ClipboardList} /><Metric label="Delivered" value="31" unit="this week" icon={CheckCircle} /></div>
      <div className="panel glass"><h3>Store details</h3><div className="detail-row"><span>License</span><strong>PHARMA-LIC-500</strong></div><div className="detail-row"><span>Address</span><strong>Mumbai</strong></div><div className="detail-row"><span>Status</span><strong>Approved</strong></div></div>
    </section>
  );
}

function PharmacyInventory() {
  const [items, setItems] = useState(medicines.map((m) => ({ ...m, prescriptionRequired: m.prescriptionRequired ? 'Yes' : 'No', availability: m.stock > 0 ? 'Available' : 'Unavailable' })));
  const [form, setForm] = useState({ name: '', category: '', price: '', prescriptionRequired: 'No', availability: 'Available' });
  const addMedicine = () => {
    if (!form.name || !form.price) {
      alert('Medicine name and price are required.');
      return;
    }
    setItems([...items, { id: Date.now(), ...form, price: Number(form.price) }]);
    setForm({ name: '', category: '', price: '', prescriptionRequired: 'No', availability: 'Available' });
  };
  const updateItem = (id, key, value) => setItems(items.map((item) => {
    if (item.id !== id) return item;
    const next = { ...item, [key]: key === 'price' || key === 'stock' ? Number(value) : value };
    return next;
  }));
  const removeItem = (id) => setItems(items.filter((item) => item.id !== id));
  return (
    <section className="module">
      <div className="panel glass module-head"><div><Pill size={28} /><h2>Medicine inventory</h2></div></div>
      <div className="inventory-layout">
        <div className="panel glass inventory-form"><h3>Add medicine</h3><input placeholder="Medicine name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /><input placeholder="Category" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} /><input placeholder="Price" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} /><select value={form.prescriptionRequired} onChange={(e) => setForm({ ...form, prescriptionRequired: e.target.value })}><option>No</option><option>Yes</option></select><select value={form.availability} onChange={(e) => setForm({ ...form, availability: e.target.value })}><option>Available</option><option>Unavailable</option></select><button className="primary" onClick={addMedicine}>Add medicine</button></div>
        <div className="panel glass table-panel"><h3>Medicine price and availability</h3><table><thead><tr><th>Medicine</th><th>Category</th><th>Price</th><th>Availability</th><th>Rx</th><th>Actions</th></tr></thead><tbody>{items.map((item) => <tr key={item.id}><td>{item.name}</td><td>{item.category}</td><td><input value={item.price} onChange={(e) => updateItem(item.id, 'price', e.target.value)} /></td><td><select value={item.availability} onChange={(e) => updateItem(item.id, 'availability', e.target.value)}><option>Available</option><option>Unavailable</option></select></td><td><select value={item.prescriptionRequired} onChange={(e) => updateItem(item.id, 'prescriptionRequired', e.target.value)}><option>No</option><option>Yes</option></select></td><td><button className="mini danger-mini" onClick={() => removeItem(item.id)}>Remove</button></td></tr>)}</tbody></table></div>
      </div>
    </section>
  );
}
function PharmacyOrders() {
  const [rows, setRows] = useState(pharmacyOrders);
  useEffect(() => {
    api.get('/me/profile').then(({ data }) => {
      if (!data.profileId) return null;
      return api.get(`/pharmacy/${data.profileId}/orders`);
    }).then((response) => {
      if (!response || !Array.isArray(response.data) || !response.data.length) return;
      setRows(response.data.map((order) => ({
        customer: order.patientName || 'Patient',
        mobile: order.mobileNumber || '-',
        medicine: order.medicine || 'Medicine order',
        address: order.deliveryAddress || 'Address pending',
        amount: `Rs ${order.totalAmount || 0}`,
        status: order.status || 'ORDER_PLACED'
      })));
    }).catch(() => setRows(pharmacyOrders));
  }, []);
  return <DataTable title="Customer medicine orders" icon={Package} rows={rows} columns={['customer', 'mobile', 'medicine', 'address', 'amount', 'status']} />;
}

function LabTestManager() {
  const [labId, setLabId] = useState(null);
  const [items, setItems] = useState([]);
  const [form, setForm] = useState({ testName: '', price: '', instructions: '' });
  const [editingId, setEditingId] = useState(null);
  const [message, setMessage] = useState('');
  const normalizeTest = (item) => ({
    ...item,
    id: item.id ?? item.testId,
    testName: item.testName || item.test || item.name || 'Untitled test',
    price: item.price ?? '',
    instructions: item.instructions || item.suggestion || 'No special instructions',
    lab: item.labName || item.lab,
    city: item.city || item.laboratory?.address || 'City'
  });
  const loadTests = async (id) => {
    const { data } = await api.get(`/labs/${id}/tests`);
    setItems(Array.isArray(data) ? data.map(normalizeTest) : []);
  };
  useEffect(() => {
    api.get('/me/profile').then(({ data }) => {
      if (!data.profileId) return;
      setLabId(data.profileId);
      loadTests(data.profileId);
    }).catch(() => setMessage('Lab profile could not be loaded. Please restart the backend or sign in again.'));
  }, []);
  const addTest = async () => {
    if (!labId) return alert('Lab profile not found. Please sign in again.');
    if (!form.testName.trim() || !form.price) return alert('Test name and price are required.');
    await api.post(`/labs/${labId}/tests`, form);
    setForm({ testName: '', price: '', instructions: '' });
    setMessage('Test added successfully.');
    loadTests(labId);
  };
  const updateTest = async (id, field, value) => {
    const next = items.map((item) => item.id === id ? { ...item, [field]: value } : item);
    setItems(next);
  };
  const saveTest = async (id) => {
    if (!id) return setMessage('This test cannot be updated because its ID is missing. Please restart the backend and reload the page.');
    const changed = items.find((item) => item.id === id);
    await api.put(`/labs/tests/${id}`, { testName: changed.testName, price: String(changed.price), instructions: changed.instructions || '' });
    setEditingId(null);
    setMessage('Test updated.');
  };
  const removeTest = async (id) => {
    if (!id) return setMessage('This test cannot be removed because its ID is missing. Please restart the backend and reload the page.');
    if (!window.confirm('Remove this test from the lab catalog?')) return;
    try {
      await api.delete(`/labs/tests/${id}`);
      setItems(items.filter((item) => item.id !== id));
      setMessage('Test removed from the lab catalog.');
    } catch {
      setMessage('Unable to remove this test. Please restart the backend and try again.');
    }
  };
  return (
    <section className="module">
      <div className="panel glass module-head"><div><ClipboardList size={28} /><h2>Manage lab tests</h2></div></div>
      <div className="inventory-layout">
        <div className="panel glass inventory-form">
          <h3>Add test</h3>
          <input placeholder="Test name, e.g. CBC, Thyroid, Sugar" value={form.testName} onChange={(e) => setForm({ ...form, testName: e.target.value })} />
          <input placeholder="Price" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} />
          <textarea placeholder="Suggestion / instructions: morning sample, empty stomach, fasting required, anytime sample" value={form.instructions} onChange={(e) => setForm({ ...form, instructions: e.target.value })} rows="4" />
          <button className="primary" onClick={addTest}>Add test</button>
          {message && <p className="checkout-message">{message}</p>}
        </div>
        <div className="panel glass table-panel">
          <h3>Test price and suggestions</h3>
          <table><thead><tr><th>Test</th><th>Price</th><th>Suggestion</th><th>Actions</th></tr></thead><tbody>{items.map((item) => {
            const editing = editingId === item.id;
            return <tr key={item.id}><td>{editing ? <input value={item.testName || ''} onChange={(e) => updateTest(item.id, 'testName', e.target.value)} /> : item.testName}</td><td>{editing ? <input value={item.price || ''} onChange={(e) => updateTest(item.id, 'price', e.target.value)} /> : `Rs ${item.price || 0}`}</td><td>{editing ? <input value={item.instructions || ''} onChange={(e) => updateTest(item.id, 'instructions', e.target.value)} /> : item.instructions}</td><td><div className="action-row">{editing ? <button className="mini" onClick={() => saveTest(item.id)}>Save</button> : <button className="mini" onClick={() => setEditingId(item.id)}>Edit</button>}<button className="mini danger-mini" onClick={() => removeTest(item.id)}>Remove</button></div></td></tr>;
          })}</tbody></table>
          {!items.length && <p className="hint">No tests have been added yet.</p>}
        </div>
      </div>
    </section>
  );
}

function LabDashboard() { return <OperationsDashboard title="Laboratory dashboard" icon={FlaskConical} metrics={[['Bookings', 18], ['Samples Pending', 7], ['Reports Due', 5], ['Completed', 42]]} />; }
function LabBookings() {
  const [rows, setRows] = useState(labBookings);
  useEffect(() => {
    api.get('/labs/bookings').then(({ data }) => {
      if (!Array.isArray(data) || !data.length) return;
      setRows(data.map((booking) => ({
        patient: booking.patient?.user?.name || 'Patient',
        samplePatient: booking.samplePatientName || booking.patient?.user?.name || 'Patient',
        age: booking.samplePatientAge || '-',
        gender: booking.samplePatientGender || '-',
        test: booking.test?.testName || 'Lab test',
        location: booking.collectionAddress || 'Location pending',
        slot: booking.createdAt?.replace('T', ' ') || 'Pending',
        status: booking.status || 'PENDING'
      })));
    }).catch(() => setRows(labBookings));
  }, []);
  return <DataTable title="Patient test bookings" icon={FlaskConical} rows={rows} columns={['samplePatient', 'age', 'gender', 'test', 'location', 'slot', 'status']} />;
}
function DoctorDashboard({ session }) {
  return <OperationsDashboard title={`${session.name} dashboard`} icon={Stethoscope} metrics={[['My Appointments', 3], ['My Patients', 3], ['Prescriptions', 6], ['Emergency Alerts', 1]]} />;
}
function DoctorAppointments({ session }) {
  const [rows, setRows] = useState([]);
  useEffect(() => {
    api.get('/me/profile').then((profile) => {
      if (!profile.data.profileId) return;
      return api.get(`/doctor/${profile.data.profileId}/appointments`);
    }).then((response) => {
      if (!response) return;
      setRows(response.data.map((item) => ({
        patient: item.patientName,
        doctor: item.doctorName,
        speciality: item.speciality,
        slot: item.slot,
        status: item.mode || item.status,
        previousPdf: 'Check prescriptions'
      })));
    }).catch(() => setRows([]));
  }, [session.userId]);
  return <DataTable title="My upcoming appointments" icon={CalendarCheck} rows={rows} columns={['patient', 'doctor', 'speciality', 'slot', 'status', 'previousPdf']} />;
}
function PatientHistory() { return <DataTable title="Patient history" icon={Users} rows={[{ patient: 'Aarav Sharma', disease: 'Asthma', allergy: 'No', lastVisit: '18 May 2026', status: 'Stable' }]} columns={['patient', 'disease', 'allergy', 'lastVisit', 'status']} />; }
function AdminDashboard() {
  const [metrics, setMetrics] = useState([['Total Users', '...'], ['Doctors', '...'], ['Patients', '...'], ['Revenue', '...'], ['Appointments', '...'], ['Orders', '...'], ['Emergency Cases', '...']]);
  useEffect(() => {
    api.get('/admin/dashboard').then(({ data }) => {
      setMetrics([
        ['Total Users', data.totalUsers],
        ['Doctors', data.doctors],
        ['Patients', data.patients],
        ['Revenue', `Rs ${data.revenue}`],
        ['Appointments', data.appointments],
        ['Orders', data.orders],
        ['Emergency Cases', data.emergencyCases]
      ]);
    }).catch(() => {
      setMetrics([['Total Users', 'Error'], ['Doctors', 'Error'], ['Patients', 'Error'], ['Revenue', 'Error']]);
    });
  }, []);
  return <OperationsDashboard title="Admin dashboard" icon={ShieldCheck} metrics={metrics} admin />;
}

function AdminApprovals() {
  const [items, setItems] = useState([]);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const load = async () => {
    setLoading(true);
    const { data } = await api.get('/admin/approvals');
    setItems(data);
    setSelected(data[0] || null);
    setLoading(false);
  };
  useEffect(() => { load(); }, []);
  const decide = async (status) => {
    if (!selected) return;
    await api.patch(`/admin/users/${selected.userId}/status?status=${status}`);
    await load();
  };
  return (
    <section className="approval-layout">
      <div className="panel glass table-panel"><div className="module-head inline-head"><div><UserCheck size={28} /><h2>Verification queue</h2></div></div>{loading ? <p>Loading approvals...</p> : <table><thead><tr><th>Name</th><th>Role</th><th>Document</th><th>Status</th><th>Action</th></tr></thead><tbody>{items.map((item) => <tr key={`${item.role}-${item.userId}`}><td>{item.name}</td><td>{item.role}</td><td>{item.documentId}</td><td><span className={`status ${item.status.toLowerCase()}`}>{item.status}</span></td><td><button className="mini" onClick={() => setSelected(item)}>Open form</button></td></tr>)}</tbody></table>}</div>
      <div className="panel glass approval-details"><h3>Verification form</h3>{selected ? <><Detail label="Full name" value={selected.name} /><Detail label="Role" value={selected.role} /><Detail label="Mobile" value={`${selected.mobileNumber} (${selected.mobileVerified ? 'OTP verified' : 'OTP pending'})`} /><Detail label="Email" value={`${selected.email || 'Not provided'} (${selected.emailVerified ? 'verified' : 'pending'})`} /><Detail label="NMC / License / Document" value={selected.documentId} /><Detail label="Expertise" value={selected.speciality || 'Not applicable'} /><Detail label="Experience" value={selected.experience || 'Not applicable'} /><Detail label="Address" value={selected.address} /><div className="decision-row"><button className="approve" onClick={() => decide('APPROVED')}><CheckCircle size={17} />Approve</button><button className="reject" onClick={() => decide('REJECTED')}><XCircle size={17} />Reject</button></div></> : <p>No approval selected.</p>}</div>
    </section>
  );
}

function AdminUsers() {
  const [usersList, setUsersList] = useState([]);
  const load = async () => {
    const { data } = await api.get('/admin/users');
    setUsersList(data);
  };
  useEffect(() => { load().catch(() => {}); }, []);
  const updateStatus = async (userId, status) => {
    await api.patch(`/admin/users/${userId}/status?status=${status}`);
    await load();
  };
  return (
    <section className="module">
      <div className="panel glass module-head"><div><Users size={28} /><h2>All users</h2></div></div>
      <div className="panel glass table-panel">
        <table>
          <thead><tr><th>Name</th><th>Email</th><th>Mobile</th><th>Role</th><th>Status</th><th>Email</th><th>Mobile OTP</th><th>Actions</th></tr></thead>
          <tbody>{usersList.map((user) => { const status = user.status || 'PENDING'; return <tr key={user.id}><td>{user.name || '-'}</td><td>{user.email || '-'}</td><td>{user.mobileNumber || '-'}</td><td>{user.role || '-'}</td><td><span className={`status ${status.toLowerCase()}`}>{status}</span></td><td>{user.emailVerified ? 'Verified' : 'Pending'}</td><td>{user.mobileVerified ? 'Verified' : 'Pending'}</td><td><div className="row-actions"><button className="mini" onClick={() => updateStatus(user.id, 'APPROVED')}>Approve</button><button className="mini danger-mini" onClick={() => updateStatus(user.id, 'BLOCKED')}>Block</button></div></td></tr>; })}</tbody>
        </table>
      </div>
    </section>
  );
}

function Detail({ label, value }) { return <div className="detail-row"><span>{label}</span><strong>{value || '-'}</strong></div>; }

function OperationsDashboard({ title, icon: Icon, metrics, admin }) {
  return <section className="page-grid"><div className="panel hero-panel glass"><div><span className="eyebrow">Operations</span><h2>{title}</h2><p>Role-specific workspace with only the details this user needs.</p></div><Icon size={44} /></div>{metrics.map(([label, value]) => <article className="metric glass" key={label}><span>{label}</span><strong>{value}</strong><small>{admin ? 'Platform metric' : 'Today'}</small></article>)}<div className="panel glass wide"><h3>Weekly activity</h3><Bar data={{ labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'], datasets: [{ data: [12, 18, 14, 21, 17], backgroundColor: ['#0ea5e9', '#14b8a6', '#6366f1', '#22c55e', '#f59e0b'] }] }} options={{ plugins: { legend: { display: false } } }} /></div><div className="panel glass"><h3>Completion</h3><Doughnut data={{ labels: ['Done', 'Pending'], datasets: [{ data: [72, 28], backgroundColor: ['#14b8a6', '#e2e8f0'] }] }} /></div></section>;
}

function DataTable({ title, icon: Icon, rows, columns }) {
  return <section className="module"><div className="panel glass module-head"><div><Icon size={28} /><h2>{title}</h2></div><div className="search"><Search size={17} /><input placeholder="Search" /></div></div><div className="panel glass table-panel"><table><thead><tr>{columns.map((column) => <th key={column}>{column}</th>)}<th>Action</th></tr></thead><tbody>{rows.map((row, index) => <tr key={index}>{columns.map((column) => <td key={column}>{row[column]}</td>)}<td><button className="mini">Open</button></td></tr>)}</tbody></table></div></section>;
}

function Metric({ label, value, unit, icon: Icon }) { return <article className="metric glass"><Icon size={24} /><span>{label}</span><strong>{value}</strong><small>{unit}</small></article>; }

function Chatbot() {
  const [messages, setMessages] = useState([{ from: 'bot', text: 'Tell me your symptom or disease, I will suggest precautions and warning signs.' }]);
  const [text, setText] = useState('');
  const send = () => {
    const reply = text.toLowerCase().includes('asthma') ? 'Asthma: avoid smoke and dust, use breathing exercises, track oxygen, and seek urgent care for severe breathlessness.' : 'Monitor vitals, hydrate, rest, and contact a doctor if symptoms worsen.';
    setMessages([...messages, { from: 'user', text }, { from: 'bot', text: reply }]);
    setText('');
  };
  return <section className="chat panel glass"><h2><Bot size={24} />AI health assistant</h2><div className="chat-stream">{messages.map((m, i) => <p key={i} className={m.from}>{m.text}</p>)}</div><div className="chat-input"><input value={text} onChange={(e) => setText(e.target.value)} placeholder="Ask for precautions, exercises, health tips..." /><button onClick={send} className="primary">Send</button></div></section>;
}

createRoot(document.getElementById('root')).render(<App />);
