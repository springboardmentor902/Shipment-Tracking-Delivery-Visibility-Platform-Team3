import React from "react";
import api from "../lib/api";

const Reports = () => {
  const downloadReport = async (reportType, format) => {
    try {
      const response = await api.get(`/reports/${reportType}`, {
        params: { format },
        responseType: "blob",
      });

      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);

      const link = document.createElement("a");
      link.href = url;
      link.download = `${reportType}-report.${format === "excel" ? "xlsx" : "pdf"}`;

      document.body.appendChild(link);
      link.click();
      link.remove();

      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error("Report download failed:", error);
      alert("Unable to generate report.");
    }
  };

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">Reports Management</h1>

      <div className="space-y-4">
        <ReportRow
          title="Shipment Report"
          onPdf={() => downloadReport("shipments", "pdf")}
          onExcel={() => downloadReport("shipments", "excel")}
        />

        <ReportRow
          title="Delivery Report"
          onPdf={() => downloadReport("delivery", "pdf")}
          onExcel={() => downloadReport("delivery", "excel")}
        />

        <ReportRow
          title="Route Performance Report"
          onPdf={() => downloadReport("route-performance", "pdf")}
          onExcel={() => downloadReport("route-performance", "excel")}
        />

        <ReportRow
          title="Delay Analysis Report"
          onPdf={() => downloadReport("delay-analysis", "pdf")}
          onExcel={() => downloadReport("delay-analysis", "excel")}
        />
      </div>
    </div>
  );
};

const ReportRow = ({ title, onPdf, onExcel }) => {
  return (
    <div className="border rounded-lg p-4 flex justify-between items-center">
      <h2 className="font-semibold">{title}</h2>

      <div className="flex gap-2">
        <button
          onClick={onPdf}
          className="bg-red-600 text-white px-4 py-2 rounded"
        >
          PDF
        </button>

        <button
          onClick={onExcel}
          className="bg-green-600 text-white px-4 py-2 rounded"
        >
          Excel
        </button>
      </div>
    </div>
  );
};

export default Reports;