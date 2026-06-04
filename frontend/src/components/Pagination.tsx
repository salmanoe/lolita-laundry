interface Props {
  page: number // 0-based
  totalPages: number
  totalElements: number
  onPage: (page: number) => void
}

/** Simple Prev/Next pager shown as a table footer. Hidden when there is no data. */
export default function Pagination({ page, totalPages, totalElements, onPage }: Props) {
  if (totalElements === 0) return null

  const btn =
    'rounded-md border border-gray-300 px-3 py-1 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40'

  return (
    <div className="flex items-center justify-between border-t bg-white px-4 py-3 text-sm">
      <span className="text-gray-500">
        Halaman {page + 1} dari {Math.max(totalPages, 1)} · {totalElements} data
      </span>
      <div className="flex gap-2">
        <button className={btn} disabled={page <= 0} onClick={() => onPage(page - 1)}>
          Sebelumnya
        </button>
        <button className={btn} disabled={page >= totalPages - 1} onClick={() => onPage(page + 1)}>
          Berikutnya
        </button>
      </div>
    </div>
  )
}
