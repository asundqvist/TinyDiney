import React from 'react';

export default function Pagination({ page, totalPages, total, onPageChange, label, disabled = false }) {
  if (!total) return null;
  return <nav className="pagination" aria-label={label}>
    <button className="button outline small" disabled={disabled || page === 0} onClick={() => onPageChange(page - 1)}>Previous</button>
    <span role="status">Page {page + 1} of {totalPages}</span>
    <button className="button outline small" disabled={disabled || page + 1 >= totalPages} onClick={() => onPageChange(page + 1)}>Next</button>
  </nav>;
}
