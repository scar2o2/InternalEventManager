import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";

export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const { showToast } = useToast();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    try {
      const data = await login(form);
      showToast({
        title: "Login successful",
        message: `Welcome back, ${data.name}.`,
        variant: "success"
      });
      navigate(data.role === "ADMIN" ? "/admin/dashboard" : "/events");
    } catch (err) {
      const message = err.response?.data?.message || "Unable to login right now.";
      setError(message);
      showToast({ title: "Login failed", message, variant: "error" });
    }
  };

  return (
    <section className="auth-page">
      <div className="hero-panel">
        <p className="eyebrow">Authentication Required</p>
        <h1>Internal Department Event Ticket Booking System</h1>
        <p>Login is the entry gate. No event page, booking form, or dashboard loads before authentication.</p>
      </div>
      <form className="brutal-card auth-card" onSubmit={handleSubmit}>
        <h2>Login</h2>
        <input placeholder="Email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <input placeholder="Password" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        {error ? <p className="error-text">{error}</p> : null}
        <button className="brutal-button" type="submit">Enter System</button>
        <p>Need an account? <Link to="/register">Register</Link></p>
        <div className="demo-credentials">
          <span>Admin: `admin@department.edu` / `Admin@123`</span>
          <span>User: `student@department.edu` / `Student@123`</span>
        </div>
      </form>
    </section>
  );
}
