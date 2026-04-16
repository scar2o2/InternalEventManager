import { useEffect, useState } from "react";
import {
  BarChart, Bar, CartesianGrid, Cell, Legend, Line, LineChart,
  Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis
} from "recharts";
import api from "../services/api";

const colors = ["#ff6b00", "#00d4ff", "#f4dd00", "#ff3b7a", "#8bf400"];

export default function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadDashboard = () => {
      api.get("/admin/dashboard")
        .then(({ data }) => setDashboard(data))
        .catch((err) => setError(err.response?.data?.message || "Unable to load dashboard."));
    };

    loadDashboard();
    const intervalId = window.setInterval(loadDashboard, 15000);
    return () => window.clearInterval(intervalId);
  }, []);

  if (!dashboard) {
    return <p>{error || "Loading dashboard..."}</p>;
  }

  return (
    <section className="dashboard-grid">
      <article className="brutal-card metric-card"><span>Total Events</span><strong>{dashboard.totalEvents}</strong></article>
      <article className="brutal-card metric-card"><span>Total Bookings</span><strong>{dashboard.totalBookings}</strong></article>
      <article className="brutal-card metric-card"><span>Total Users</span><strong>{dashboard.totalUsers}</strong></article>

      <article className="brutal-card chart-card">
        <h2>Total Bookings per Event</h2>
        <ResponsiveContainer width="100%" height={280}>
          <BarChart data={dashboard.bookingsPerEvent}>
            <CartesianGrid stroke="#111" />
            <XAxis dataKey="name" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="bookings" fill="#ff6b00" />
          </BarChart>
        </ResponsiveContainer>
      </article>

      <article className="brutal-card chart-card">
        <h2>Ticket Distribution</h2>
        <ResponsiveContainer width="100%" height={280}>
          <PieChart>
            <Pie data={dashboard.ticketDistribution} dataKey="tickets" nameKey="name" outerRadius={95}>
              {dashboard.ticketDistribution.map((entry, index) => (
                <Cell key={entry.name} fill={colors[index % colors.length]} />
              ))}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </article>

      <article className="brutal-card chart-card">
        <h2>Remaining vs Booked Tickets</h2>
        <ResponsiveContainer width="100%" height={280}>
          <BarChart data={dashboard.remainingVsBooked}>
            <CartesianGrid stroke="#111" />
            <XAxis dataKey="name" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Bar dataKey="booked" fill="#111" />
            <Bar dataKey="remaining" fill="#00d4ff" />
          </BarChart>
        </ResponsiveContainer>
      </article>

      <article className="brutal-card chart-card">
        <h2>Bookings Over Time</h2>
        <ResponsiveContainer width="100%" height={280}>
          <LineChart data={dashboard.bookingsOverTime}>
            <CartesianGrid stroke="#111" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip />
            <Line type="monotone" dataKey="tickets" stroke="#ff3b7a" strokeWidth={4} />
          </LineChart>
        </ResponsiveContainer>
      </article>
    </section>
  );
}
