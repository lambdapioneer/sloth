## COMMIT changes and TAG the release

```
git add .
git commit --allow-empty -m "Updated version to v<<CHANGEHERE>>"
git tag -a v<<CHANGEHERE>> <<COMMITIDENTIFIER>>
```

## PUSH to GitHub

```
git push
git push origin --tags
```
