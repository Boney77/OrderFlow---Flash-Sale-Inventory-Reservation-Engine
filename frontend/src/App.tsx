import { Routes, Route } from 'react-router-dom';
import { FlashSalePage } from './pages/FlashSalePage';
import { DashboardPage } from './pages/DashboardPage';

function App() {
  return (
    <Routes>
      <Route path="/" element={<FlashSalePage />} />
      <Route path="/dashboard" element={<DashboardPage />} />
    </Routes>
  );
}

export default App;
