defmodule FlixBackendWeb.ChangesetView do
  # 直接定义视图，避免使用宏

  def render("error.json", %{changeset: changeset}) do
    %{
      success: false,
      errors: translate_errors(changeset)
    }
  end

  defp translate_errors(changeset) do
    Ecto.Changeset.traverse_errors(changeset, &translate_error/1)
  end

  defp translate_error({msg, opts}) do
    # 当使用 gettext 时：
    # Gettext.dngettext(FlixBackendWeb.Gettext, "errors", msg, msg, opts)

    # 简化版本:
    Enum.reduce(opts, msg, fn {key, value}, acc ->
      String.replace(acc, "%{#{key}}", to_string(value))
    end)
  end
end
