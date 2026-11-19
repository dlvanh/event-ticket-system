import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../core/models';

export interface ChatMessage {
  from: 'user' | 'bot';
  text: string;
}

@Injectable({ providedIn: 'root' })
export class ChatbotService {
  private readonly apiUrl = environment.apiUrl;
  private readonly storageKey = 'chatMessages';
  private messages: ChatMessage[] = [];

  constructor(private http: HttpClient) {
    this.loadMessages();
  }

  private loadMessages(): void {
    try {
      const saved = sessionStorage.getItem(this.storageKey);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed)) {
          this.messages = parsed;
        }
      }
    } catch {
      sessionStorage.removeItem(this.storageKey);
      this.messages = [];
    }
  }

  getMessages(): ChatMessage[] {
    return this.messages;
  }

  addUserMessage(text: string): void {
    this.messages.push({ from: 'user', text });
    this.saveMessages();
  }

  addBotMessage(text: string): void {
    this.messages.push({ from: 'bot', text });
    this.saveMessages();
  }

  chat(message: string): Observable<ApiResponse<{ reply: string }>> {
    return this.http.post<ApiResponse<{ reply: string }>>(`${this.apiUrl}/chat`, { message }).pipe(
      tap((res: any) => {
        const reply = res?.data?.reply ?? res?.reply ?? 'Xin lỗi, tôi chưa thể phản hồi lúc này.';
        this.addBotMessage(reply);
      })
    );
  }

  saveMessages(): void {
    try {
      sessionStorage.setItem(this.storageKey, JSON.stringify(this.messages));
    } catch {
      // Ignore storage quota errors
    }
  }

  clearMessages(): void {
    this.messages = [];
    sessionStorage.removeItem(this.storageKey);
  }
}