import { parseResponseBody, toStockPayload } from './StockEdit';

describe('toStockPayload', () => {
  it('sends quantity and price as numbers and an upper-case currency', () => {
    const payload = toStockPayload({
      id: '6adcce61418f71b2ae00e072',
      name: 'Keyboard',
      quantity: '12',
      category: 'Peripherals',
      price: '49.90',
      currency: 'eur',
      createdByUser: 'admin@gmail.com'
    });

    expect(payload).toEqual({
      name: 'Keyboard',
      quantity: 12,
      category: 'Peripherals',
      price: 49.9,
      currency: 'EUR'
    });
  });

  it('leaves out id and audit fields, which dotnet-service sets itself', () => {
    const payload = toStockPayload({ id: 'x', createdDate: 'y', lastModifiedByUser: 'z', name: 'n', quantity: 0, category: 'c', price: 0, currency: 'USD' });

    expect(Object.keys(payload)).toEqual(['name', 'quantity', 'category', 'price', 'currency']);
  });
});

describe('parseResponseBody', () => {
  const forbidden = { status: 403, statusText: 'Forbidden' };

  it('turns an empty body into an error with the status', () => {
    expect(parseResponseBody('', forbidden)).toEqual({ error: '403 - Forbidden' });
  });

  it('parses JSON bodies', () => {
    expect(parseResponseBody('{"error":"Stock not found"}', { status: 404 })).toEqual({ error: 'Stock not found' });
  });

  it('keeps plain-text bodies as the error', () => {
    expect(parseResponseBody('Bad Gateway', { status: 502 })).toEqual({ error: 'Bad Gateway' });
  });
});
