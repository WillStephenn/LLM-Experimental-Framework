/**
 * Tests for Axios API Client
 *
 * @module api/client.test
 */
import { describe, it, expect, beforeEach, afterEach, vi, type Mock } from 'vitest';
import { type AxiosResponse } from 'axios';
import { api, apiClient, ApiError, type ApiErrorResponse } from './client';

describe('ApiError', () => {
  it('should create an ApiError with all properties', () => {
    const errorResponse: ApiErrorResponse = {
      timestamp: '2025-11-27T10:00:00Z',
      status: 400,
      error: 'Bad Request',
      message: 'Validation failed',
      path: '/api/tasks',
      fieldErrors: [{ field: 'name', message: 'must not be blank' }],
    };

    const error = new ApiError(errorResponse);

    expect(error.name).toBe('ApiError');
    expect(error.message).toBe('Validation failed');
    expect(error.status).toBe(400);
    expect(error.errorType).toBe('Bad Request');
    expect(error.path).toBe('/api/tasks');
    expect(error.timestamp).toBe('2025-11-27T10:00:00Z');
    expect(error.fieldErrors).toEqual([{ field: 'name', message: 'must not be blank' }]);
  });

  it('should create an ApiError without field errors', () => {
    const errorResponse: ApiErrorResponse = {
      timestamp: '2025-11-27T10:00:00Z',
      status: 404,
      error: 'Not Found',
      message: 'Resource not found',
      path: '/api/tasks/999',
    };

    const error = new ApiError(errorResponse);

    expect(error.status).toBe(404);
    expect(error.fieldErrors).toBeUndefined();
  });

  it('should be an instance of Error', () => {
    const errorResponse: ApiErrorResponse = {
      timestamp: '2025-11-27T10:00:00Z',
      status: 500,
      error: 'Internal Server Error',
      message: 'Something went wrong',
      path: '/api/tasks',
    };

    const error = new ApiError(errorResponse);

    expect(error).toBeInstanceOf(Error);
    expect(error).toBeInstanceOf(ApiError);
  });
});

describe('apiClient', () => {
  it('should have apiClient defined', () => {
    expect(apiClient).toBeDefined();
  });

  it('should have api convenience object defined with all methods', () => {
    expect(api).toBeDefined();
    expect(api.get).toBeDefined();
    expect(api.post).toBeDefined();
    expect(api.put).toBeDefined();
    expect(api.delete).toBeDefined();
    expect(api.patch).toBeDefined();
    expect(typeof api.get).toBe('function');
    expect(typeof api.post).toBe('function');
    expect(typeof api.put).toBe('function');
    expect(typeof api.delete).toBe('function');
    expect(typeof api.patch).toBe('function');
  });
});

describe('api convenience methods', () => {
  const mockResponse = {
    data: { id: 1, name: 'Test' },
  } as AxiosResponse<{ id: number; name: string }>;
  let getMock: Mock;
  let postMock: Mock;
  let putMock: Mock;
  let deleteMock: Mock;
  let patchMock: Mock;

  beforeEach(() => {
    vi.clearAllMocks();
    getMock = vi.fn().mockResolvedValue(mockResponse);
    postMock = vi.fn().mockResolvedValue(mockResponse);
    putMock = vi.fn().mockResolvedValue(mockResponse);
    deleteMock = vi.fn().mockResolvedValue(mockResponse);
    patchMock = vi.fn().mockResolvedValue(mockResponse);

    vi.spyOn(apiClient, 'get').mockImplementation(getMock);
    vi.spyOn(apiClient, 'post').mockImplementation(postMock);
    vi.spyOn(apiClient, 'put').mockImplementation(putMock);
    vi.spyOn(apiClient, 'delete').mockImplementation(deleteMock);
    vi.spyOn(apiClient, 'patch').mockImplementation(patchMock);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('api.get should call apiClient.get and return data', async () => {
    const result = await api.get<{ id: number; name: string }>('/tasks');

    expect(getMock).toHaveBeenCalledWith('/tasks', undefined);
    expect(result).toEqual({ id: 1, name: 'Test' });
  });

  it('api.get should pass config options', async () => {
    const config = { params: { page: 1 } };
    await api.get('/tasks', config);

    expect(getMock).toHaveBeenCalledWith('/tasks', config);
  });

  it('api.post should call apiClient.post and return data', async () => {
    const body = { name: 'New Task' };
    const result = await api.post<{ id: number; name: string }>('/tasks', body);

    expect(postMock).toHaveBeenCalledWith('/tasks', body, undefined);
    expect(result).toEqual({ id: 1, name: 'Test' });
  });

  it('api.put should call apiClient.put and return data', async () => {
    const body = { name: 'Updated Task' };
    const result = await api.put<{ id: number; name: string }>('/tasks/1', body);

    expect(putMock).toHaveBeenCalledWith('/tasks/1', body, undefined);
    expect(result).toEqual({ id: 1, name: 'Test' });
  });

  it('api.delete should call apiClient.delete and return data', async () => {
    const result = await api.delete<{ id: number; name: string }>('/tasks/1');

    expect(deleteMock).toHaveBeenCalledWith('/tasks/1', undefined);
    expect(result).toEqual({ id: 1, name: 'Test' });
  });

  it('api.patch should call apiClient.patch and return data', async () => {
    const body = { name: 'Patched Task' };
    const result = await api.patch<{ id: number; name: string }>('/tasks/1', body);

    expect(patchMock).toHaveBeenCalledWith('/tasks/1', body, undefined);
    expect(result).toEqual({ id: 1, name: 'Test' });
  });
});
