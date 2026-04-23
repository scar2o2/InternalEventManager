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
  const [seats, setSeats] = useState([]);
  const [selectedSeatIds, setSelectedSeatIds] = useState([]);
  const [holdInfo, setHoldInfo] = useState(null);
  const [error, setError] = useState("");
  const [showConfirm, setShowConfirm] = useState(false);
  const [successModal, setSuccessModal] = useState({ open: false, message: "" });

  useEffect(() => {
    Promise.all([
      api.get(`/events/${id}`),
      api.get(`/events/${id}/seats`)
    ])
      .then(([eventResponse, seatResponse]) => {
        setEvent(eventResponse.data);
        setSeats(seatResponse.data);
      })
      .catch((err) => {
        const message = err.response?.data?.message || "Unable to load event.";
        setError(message);
        showToast({ title: "Event unavailable", message, variant: "error" });
      });
  }, [id, showToast]);

  const toggleSeat = (seat) => {
    if (seat.status === "BOOKED" || (seat.status === "HELD" && !seat.heldByCurrentUser)) {
      return;
    }
    setSelectedSeatIds((current) => (
      current.includes(seat.id)
        ? current.filter((seatId) => seatId !== seat.id)
        : [...current, seat.id]
    ));
  };

  const holdSeats = async () => {
    if (!selectedSeatIds.length) {
      setError("Select at least one seat to continue.");
      return;
    }
    try {
      const { data } = await api.post("/bookings/hold", { eventId: Number(id), seatIds: selectedSeatIds });
      setHoldInfo(data);
      showToast({
        title: "Seats held",
        message: `${data.seatNumbers.join(", ")} held until ${new Date(data.holdExpiresAt).toLocaleTimeString()}.`,
        variant: "info"
      });
      const { data: refreshedSeats } = await api.get(`/events/${id}/seats`);
      const { data: refreshedEvent } = await api.get(`/events/${id}`);
      setSeats(refreshedSeats);
      setEvent(refreshedEvent);
    } catch (err) {
      const message = err.response?.data?.message || "Unable to hold seats.";
      setError(message);
      showToast({ title: "Hold failed", message, variant: "error" });
    }
  };

  const confirmBooking = async () => {
    setError("");
    setShowConfirm(false);
    try {
      const { data } = await api.post("/bookings", { eventId: Number(id), seatIds: selectedSeatIds });
      setSuccessModal({
        open: true,
        message: `Booking confirmed for ${data.tickets} ticket(s). Seats: ${data.seatNumbers.join(", ")}. Total: Rs. ${data.totalAmount}`
      });
      showToast({
        title: "Tickets booked",
        message: `${data.eventName} is now in your booking history.`,
        variant: "success"
      });
      const { data: refreshedSeats } = await api.get(`/events/${id}/seats`);
      const { data: refreshedEvent } = await api.get(`/events/${id}`);
      setSeats(refreshedSeats);
      setEvent(refreshedEvent);
      setSelectedSeatIds([]);
      setHoldInfo(null);
    } catch (err) {
      const message = err.response?.data?.message || "Booking failed.";
      setError(message);
      showToast({ title: "Booking failed", message, variant: "error" });
    }
  };

  const joinWaitlist = async () => {
    try {
      const { data } = await api.post("/bookings/waitlist", {
        eventId: Number(id),
        tickets: Math.max(1, selectedSeatIds.length)
      });
      showToast({ title: "Waitlist joined", message: data.message, variant: "info" });
    } catch (err) {
      const message = err.response?.data?.message || "Unable to join waitlist.";
      setError(message);
      showToast({ title: "Waitlist failed", message, variant: "error" });
    }
  };

  if (!event) {
    return <p>{error || "Loading booking form..."}</p>;
  }

  return (
    <section className="brutal-card booking-panel">
      <p className="eyebrow">User Booking Only</p>
      <h1>{event.name}</h1>
      <div className="booking-layout">
        <div className="seat-selector brutal-card">
          <h2>Select seats</h2>
          <div className="seat-legend">
            <span><i className="seat-swatch available" /> Available</span>
            <span><i className="seat-swatch booked" /> Booked</span>
            <span><i className="seat-swatch held" /> Held</span>
            <span><i className="seat-swatch selected" /> Selected</span>
          </div>
          <div className="seat-grid">
            {seats.map((seat) => {
              const isSelected = selectedSeatIds.includes(seat.id);
              const className = [
                "seat-button",
                seat.status.toLowerCase(),
                isSelected ? "selected" : "",
                seat.heldByCurrentUser ? "mine" : ""
              ].join(" ").trim();
              return (
                <button
                  key={seat.id}
                  type="button"
                  className={className}
                  onClick={() => toggleSeat(seat)}
                >
                  {seat.seatNumber}
                </button>
              );
            })}
          </div>
        </div>
        <form
          onSubmit={(submitEvent) => {
            submitEvent.preventDefault();
            setShowConfirm(true);
          }}
          className="booking-form"
        >
          <div className="summary-box">
            <span>Base price: Rs. {event.ticketPrice}</span>
            <span>Live price: Rs. {holdInfo?.pricePerTicket || event.currentTicketPrice}</span>
            <span>Remaining tickets: {event.availableTickets}</span>
            <span>Selected seats: {selectedSeatIds.length}</span>
            <span>Total amount: Rs. {(holdInfo?.totalAmount || Number(event.currentTicketPrice) * selectedSeatIds.length) || 0}</span>
            {holdInfo ? <span>Hold expires: {new Date(holdInfo.holdExpiresAt).toLocaleTimeString()}</span> : null}
          </div>
          <div className="card-actions">
            <button className="brutal-button secondary" type="button" onClick={holdSeats}>
              Hold Seats
            </button>
            <button className="brutal-button" type="submit" disabled={!selectedSeatIds.length}>
              Confirm Booking
            </button>
          </div>
          <button className="brutal-button danger" type="button" onClick={joinWaitlist}>
            Join Waitlist
          </button>
          <p className="muted-text">Yellow seats are temporarily locked. Your own held seats stay selectable.</p>
        </form>
      </div>
      {error ? <p className="error-text">{error}</p> : null}
      <Modal
        open={showConfirm}
        title="Confirm booking"
        message={`Book ${selectedSeatIds.length} seat(s) for ${event.name} for Rs. ${holdInfo?.totalAmount || Number(event.currentTicketPrice) * selectedSeatIds.length}?`}
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
