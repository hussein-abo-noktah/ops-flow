interface PaginationProps {
    page: number;
    totalPages: number;
    first: boolean;
    last: boolean;
    disabled?: boolean;
    onPageChange:
        (page: number) => void;
}

export function PaginationProps({page, totalPages, first, last, disabled = false, onPageChange}: PaginationProps){
    return (
        <nav className="incident-page-controls" aria-label="Incident pages">
        <button
            className="incident-button"
            disabled={first || disabled}
            onClick={() =>
                onPageChange(page - 1)
            }
        >
            Previous
        </button>

    <span>
  Page {Math.min(page + 1, Math.max(1, totalPages))} of {Math.max(1, totalPages)}
</span>

    <button
        className="incident-button"
        disabled={last || disabled}
        onClick={() =>
            onPageChange(page + 1)
        }
    >
        Next
    </button>
        </nav>
    )
}
