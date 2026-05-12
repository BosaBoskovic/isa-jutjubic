import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';

function ClickHandler({ onPick }) {
  useMapEvents({
    click(e) {
      onPick(e.latlng);
    },
  });
  return null;
}

const MapPicker = ({ value, onChange }) => {
  const center = value
    ? [value.lat, value.lng]
    : [45.2671, 19.8335]; // Novi Sad default

  return (
    <div style={{ border: '2px solid var(--border)', borderRadius: 12, overflow: 'hidden' }}>
      <MapContainer center={center} zoom={13} style={{ height: 260, width: '100%' }}>
        <TileLayer
          attribution="&copy; OpenStreetMap contributors"
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <ClickHandler onPick={(latlng) => onChange(latlng)} />

        {value && <Marker position={[value.lat, value.lng]} />}
      </MapContainer>
    </div>
  );
};

export default MapPicker;
