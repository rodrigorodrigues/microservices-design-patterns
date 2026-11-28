<?php

namespace App\Http\Controllers;

use App\Books\BookRepository;
use App\Books\DuplicateIsbnException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class BookController extends Controller
{
    public function __construct(private readonly BookRepository $books) {}

    public function index(Request $request): JsonResponse
    {
        $query = $request->validate([
            'page' => ['sometimes', 'integer', 'min:0'],
            'size' => ['sometimes', 'integer', 'min:1', 'max:100'],
        ]);
        $page = (int) ($query['page'] ?? 0);
        $size = (int) ($query['size'] ?? 10);

        $result = $this->books->page($page, $size);
        $totalPages = (int) ceil($result['totalElements'] / $size);

        return response()->json([
            'content' => $result['content'],
            'number' => $page,
            'size' => $size,
            'totalPages' => $totalPages,
            'totalElements' => $result['totalElements'],
            'first' => $page === 0,
            'last' => $page >= $totalPages - 1,
        ]);
    }

    public function show(string $id): JsonResponse
    {
        $book = $this->books->find($id);

        return $book ? response()->json($book) : $this->notFound($id);
    }

    public function store(Request $request): JsonResponse
    {
        $attributes = $this->validateBook($request);

        if ($this->books->findByIsbn($attributes['isbn']) !== null) {
            return $this->duplicateIsbn($attributes['isbn']);
        }

        try {
            $book = $this->books->create($attributes + [
                'createdByUser' => $this->user($request),
                'lastModifiedByUser' => $this->user($request),
            ]);
        } catch (DuplicateIsbnException $e) {
            return $this->duplicateIsbn($e->isbn);
        }

        return response()->json($book, Response::HTTP_CREATED)
            ->header('Location', url("/api/books/{$book['id']}"));
    }

    public function update(Request $request, string $id): JsonResponse
    {
        $attributes = $this->validateBook($request);

        $existing = $this->books->findByIsbn($attributes['isbn']);
        if ($existing !== null && $existing['id'] !== $id) {
            return $this->duplicateIsbn($attributes['isbn']);
        }

        try {
            $book = $this->books->update($id, $attributes + ['lastModifiedByUser' => $this->user($request)]);
        } catch (DuplicateIsbnException $e) {
            return $this->duplicateIsbn($e->isbn);
        }

        return $book ? response()->json($book) : $this->notFound($id);
    }

    public function destroy(string $id): JsonResponse
    {
        return $this->books->delete($id)
            ? response()->json(['message' => "Book $id deleted"])
            : $this->notFound($id);
    }

    /**
     * @return array<string, mixed>
     */
    private function validateBook(Request $request): array
    {
        return $request->validate([
            'title' => ['required', 'string', 'max:255'],
            'author' => ['required', 'string', 'max:255'],
            'isbn' => ['required', 'string', 'regex:/^(?:\d[\d-]{8,15}[\dXx])$/'],
            'publishedYear' => ['required', 'integer', 'min:0', 'max:'.((int) date('Y') + 1)],
            'price' => ['required', 'numeric', 'min:0'],
            'currency' => ['required', 'string', 'size:3'],
        ]);
    }

    private function user(Request $request): ?string
    {
        return $request->attributes->get('jwt.claims')['sub'] ?? null;
    }

    private function notFound(string $id): JsonResponse
    {
        return response()->json(['error' => 'Not Found', 'message' => "Book $id not found"], Response::HTTP_NOT_FOUND);
    }

    private function duplicateIsbn(string $isbn): JsonResponse
    {
        return response()->json(
            ['error' => 'Conflict', 'message' => "A book with ISBN $isbn already exists"],
            Response::HTTP_CONFLICT,
        );
    }
}
