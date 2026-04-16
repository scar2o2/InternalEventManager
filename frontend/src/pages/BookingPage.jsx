import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Modal from "../components/Modal";
import { useToast } from "../context/ToastContext";
import api from "../services/api";

export default function BookingPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [event, setEvent] = useState(null);
  const [tickets, setTickets] = useState(1);
  const [error, setError] = useState("");
  const [showConfirm, setShowConfirm] = useState(false);
  const [successModal, setSuccessModal] = useState({ open: false, message: "" });

  useEffect(() => {
    api.get(`/events/${id}`)
      .then(({ data }) => setEvent(data))
      .catch((err) => {
        const message = err.response?.data?.message || "Unable to load event.";
        setError(message);
        showToast({ title: "Event unavailable", message, variant: "error" });
      });
  }, [id, showToast]);

  const confirmBooking = async () => {
    setError("");
    setShowConfirm(false);
    try {
      const { data } = await api.post("/bookings", { eventId: Number(id), tickets: Number(tickets) });
      setSuccessModal({
        open: true,
        message: `Booking confirmed for ${data.tickets} ticket(s). Total: Rs. ${data.totalAmount}`
      });
      showToast({
        title: "Tickets booked",
        message: `${data.eventName} is now in your booking history.`,
        variant: "success"
      });
    } catch (err) {
      const message = err.response?.data?.message || "Booking failed.";
      setError(message);
      showToast({ title: "Booking failed", message, variant: "error" });
    }
  };

  if (!event) {
    return <p>{error || "Loading booking form..."}</p>;
  }

  return (
    <section className="brutal-card booking-panel">
      <p className="eyebrow">User Booking Only</p>
      <h1>{event.name}</h1>
      <form
        onSubmit={(submitEvent) => {
          submitEvent.preventDefault();
          setShowConfirm(true);
        }}
        className="booking-form"
      >
        <label>
          Tickets
          <input
            type="number"
            min="1"
            max={event.availableTickets}
            value={tickets}
            onChange={(e) => setTickets(e.target.value)}
          />
        </label>
        <div className="summary-box">
          <span>Price per ticket: Rs. {event.ticketPrice}</span>
          <span>Remaining tickets: {event.availableTickets}</span>
          <span>Total amount: Rs. {Number(event.ticketPrice) * Number(tickets || 0)}</span>
        </div>
        {error ? <p className="error-text">{error}</p> : null}
        <button className="brutal-button" type="submit">Confirm Booking</button>
      </form>
      <Modal
        open={showConfirm}
        title="Confirm booking"
        message={`Book ${tickets} ticket(s) for ${event.name} for Rs. ${Number(event.ticketPrice) * Number(tickets || 0)}?`}
        confirmLabel="Book Now"
        onConfirm={confirmBooking}
        onClose={() => setShowConfirm(false)}
      />
      <Modal
        open={successModal.open}
        title="Booking confirmed"
        message={successModal.message}
        confirmLabel="View My Bookings"
        hideCancel
        onConfirm={() => {
          setSuccessModal({ open: false, message: "" });
          navigate("/my-bookings", { state: { message: `Booked ${event.name} successfully.` } });
        }}
        onClose={() => setSuccessModal({ open: false, message: "" })}
      />
    </section>
  );
}
