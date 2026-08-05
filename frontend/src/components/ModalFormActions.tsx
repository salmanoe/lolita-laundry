import { ApiError } from '../api/client'

interface Props {
  /** Dismisses the modal without saving. */
  onClose: () => void
  /** The submit mutation's error — react-query gives `null` when the last submit succeeded. */
  error: unknown
  /** True while the submit is in flight; disables the button and swaps its label. */
  isPending: boolean
}

/**
 * The shared footer of every form modal: the submit-error line, then Batal / Simpan.
 *
 * Five modals (client, bank account, user, item, lookup) carried a byte-identical copy of this
 * markup, which SonarQube flagged as duplicated code. Extracting it also means the error wording
 * and the button styling can only ever change in one place.
 *
 * Renders a fragment, not a wrapper, so the two elements stay direct children of the form and its
 * `space-y-*` rhythm still applies to them.
 */
export default function ModalFormActions({ onClose, error, isPending }: Props) {
  return (
    <>
      {error != null && (
        <p className="text-sm text-red-600">
          {error instanceof ApiError ? error.detail : 'Gagal menyimpan.'}
        </p>
      )}

      <div className="flex justify-end gap-2 pt-1">
        <button
          type="button"
          onClick={onClose}
          className="rounded-md px-4 py-2 text-sm text-gray-600 hover:bg-gray-100"
        >
          Batal
        </button>
        <button
          type="submit"
          disabled={isPending}
          className="rounded-md bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-50"
        >
          {isPending ? 'Menyimpan...' : 'Simpan'}
        </button>
      </div>
    </>
  )
}
