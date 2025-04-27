defmodule FlixBackend.Repo.Migrations.AddProductsFullTextSearch do
  use Ecto.Migration

  def change do
    # 启用 unaccent 扩展以支持忽略重音符号的搜索
    execute("CREATE EXTENSION IF NOT EXISTS unaccent")

    # 添加搜索向量字段到产品表
    alter table(:products) do
      add :search_vector, :tsvector
    end

    # 创建 GIN 索引用于全文搜索
    create index(:products, [:search_vector], using: "GIN")

    # 创建更新触发器函数
    execute("""
    CREATE OR REPLACE FUNCTION products_search_vector_update() RETURNS trigger AS $$
    BEGIN
      NEW.search_vector :=
        setweight(to_tsvector('simple', unaccent(COALESCE(NEW.title, ''))), 'A') ||
        setweight(to_tsvector('simple', unaccent(COALESCE(NEW.description, ''))), 'B') ||
        setweight(to_tsvector('simple', unaccent(COALESCE(NEW.category, ''))), 'C') ||
        setweight(to_tsvector('simple', unaccent(array_to_string(COALESCE(NEW.tags, ARRAY[]::text[]), ' '))), 'D');
      RETURN NEW;
    END
    $$ LANGUAGE plpgsql;
    """)

    # 创建触发器
    execute("""
    CREATE TRIGGER products_search_vector_update_trigger
    BEFORE INSERT OR UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION products_search_vector_update();
    """)

    # 初始化现有记录的搜索向量
    execute("""
    UPDATE products SET search_vector =
      setweight(to_tsvector('simple', unaccent(COALESCE(title, ''))), 'A') ||
      setweight(to_tsvector('simple', unaccent(COALESCE(description, ''))), 'B') ||
      setweight(to_tsvector('simple', unaccent(COALESCE(category, ''))), 'C') ||
      setweight(to_tsvector('simple', unaccent(array_to_string(COALESCE(tags, ARRAY[]::text[]), ' '))), 'D');
    """)
  end
end
