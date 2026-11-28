import { ISBN_PATTERN, toBookPayload } from './BookEdit';

describe('toBookPayload', () => {
  it('sends publishedYear and price as numbers and an upper-case currency', () => {
    const payload = toBookPayload({
      id: '6adcce61418f71b2ae00e072',
      title: 'Clean Code',
      author: 'Robert C. Martin',
      isbn: '978-0132350884',
      publishedYear: '2008',
      price: '37.50',
      currency: 'usd',
      createdByUser: 'admin@gmail.com'
    });

    expect(payload).toEqual({
      title: 'Clean Code',
      author: 'Robert C. Martin',
      isbn: '978-0132350884',
      publishedYear: 2008,
      price: 37.5,
      currency: 'USD'
    });
  });

  it('leaves out id and audit fields, which php-service sets itself', () => {
    const payload = toBookPayload({ id: 'x', createdDate: 'y', lastModifiedByUser: 'z', title: 't', author: 'a', isbn: '0306406152', publishedYear: 1, price: 0, currency: 'EUR' });

    expect(Object.keys(payload)).toEqual(['title', 'author', 'isbn', 'publishedYear', 'price', 'currency']);
  });
});

describe('ISBN_PATTERN', () => {
  it.each(['978-0321125217', '9780321125217', '0-306-40615-2', '080442957X'])('accepts %s', (isbn) => {
    expect(ISBN_PATTERN.test(isbn)).toBe(true);
  });

  it.each(['abc', '123', 'X-306-40615-2', '978-0321125217-1234'])('rejects %s', (isbn) => {
    expect(ISBN_PATTERN.test(isbn)).toBe(false);
  });
});
