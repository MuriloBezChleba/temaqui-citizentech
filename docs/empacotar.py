from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

root = Path(__file__).resolve().parents[1]
output = root / "entregas"
output.mkdir(exist_ok=True)
sql = sorted((root / "database").glob("*.SQL"))


def zip_checked(name, entries):
    path = output / name
    with ZipFile(path, "w", compression=ZIP_DEFLATED) as archive:
        for file, archive_name in entries:
            archive.write(file, archive_name)
    with ZipFile(path) as archive:
        assert archive.testzip() is None
        assert set(archive.namelist()) == {archive_name for _, archive_name in entries}
        for file, archive_name in entries:
            assert archive.read(archive_name) == file.read_bytes()
    print(path.name, path.stat().st_size)


assert len(sql) == 3
zip_checked("TemAqui-Database.zip", [
    (root / "docs/TemAqui-Database.pdf", "TemAqui-Database.pdf"),
    *[(p, p.name) for p in sql],
])
java = [
    root / "README.md", root / "prompts.md", root / "render.yaml",
    root / "java/pom.xml", root / "java/Dockerfile", root / "java/.dockerignore",
    root / "java/verificar-api.ps1", root / "java/PrepararOracle.java", *sql,
    *sorted(p for p in (root / "java/src").rglob("*") if p.is_file()),
]
zip_checked("TemAqui-Java.zip", [
    (root / "docs/TemAqui-Java.pdf", "docs/TemAqui-Java.pdf"),
    *[(p, p.relative_to(root).as_posix()) for p in java],
])

