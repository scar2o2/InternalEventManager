import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import { useToast } from "../context/ToastContext";
import api from "../services/api";

export default function MyBookingsPage() {
  const location = useLocation();
  const { showToast } = useToast();
  const [bookings, setBookings] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    if (location.state?.message) {
      showToast({ title: "Booking updated", message: location.state.message, variant: "success" });
    }
  }, [location.state, showToast]);

  useEffect(() => {
    api.get("/bookings/me")
      .then(({ data }) => setBookings(data))
      .catch((err) => {
        const message = err.response?.data?.message || "Unable to load bookings.";
        setError(message);
        showToast({ title: "Could not load bookings", message, variant: "error" });
      });
  }, [showToast]);

  return (
    <section>
      <div className="section-banner">
        <div>
          <p className="eyebrow">Booking Summary</p>
          <h1>My bookings</h1>
        </div>
      </div>
      {error ? <p className="error-text">{error}</p> : null}
      <div className="list-stack">
        {bookings.map((booking) => (
          <article key={booking.id} className="brutal-card list-item">
            <h3>{booking.eventName}</h3>
            <div className="meta-grid">
              <span>Tickets: {booking.tickets}</span>
              <span>Total: Rs. {booking.totalAmount}</span>
              <span>Booked: {new Date(booking.bookingDate).toLocaleString()}</span>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
