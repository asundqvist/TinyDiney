export async function loadAllPages(request, path) {
  let page = 0, items = [], result;
  do {
    result = await request(path + '&page=' + page++);
    items = [...items, ...result.content];
  } while (page < result.totalPages);
  return items;
}
