import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";

export default function RegisterPage() {
  const navigate = useNavigate();
  const { register } = useAuth();
  const { showToast } = useToast();
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    department: "",
    role: "USER"
  });
  const [error, setError] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    try {
      const data = await register(form);
      showToast({
        title: "Registration complete",
        message: "Your account is ready to use.",
        variant: "success"
      });
      navigate(data.role === "ADMIN" ? "/admin/dashboard" : "/events");
    } catch (err) {
      const message = err.response?.data?.message || "Unable to register right now.";
      setError(message);
      showToast({ title: "Registration failed", message, variant: "error" });
    }
  };

  return (
    <section className="auth-page">
      <div className="hero-panel">
        <p className="eyebrow">Secure Onboarding</p>
        <h1>Create your department access pass</h1>
        <p>Students and faculty can register and immediately enter the protected event system.</p>
      </div>
      <form className="brutal-card auth-card" onSubmit={handleSubmit}>
        <h2>Register</h2>
        <input placeholder="Full name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <input placeholder="Department" value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })} />
        <input placeholder="Email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <input placeholder="Password" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        <div className="summary-box">
          <span>Role assignment: Student / Faculty user</span>
          <span>Admin access is provisioned separately.</span>
        </div>
        {error ? <p className="error-text">{error}</p> : null}
        <button className="brutal-button" type="submit">Create Account</button>
        <p>Already registered? <Link to="/login">Login</Link></p>
      </form>
    </section>
  );
}
