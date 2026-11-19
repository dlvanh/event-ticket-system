import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { ChatbotService, ChatMessage } from '../../services/chatbot.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';

@Component({
  selector: 'app-chatbot',
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.css']
})
export class ChatbotComponent implements OnInit {
  isOpen = false;
  isLoading = false;
  showSuggestions = false;
  userMessage = '';
  messages: ChatMessage[] = [];
  @ViewChild('chatContainer') private chatContainer!: ElementRef;

  constructor(private chatbotService: ChatbotService) {}

  ngOnInit() {
    this.messages = this.chatbotService.getMessages();
  }

  toggleChat() {
    this.isOpen = !this.isOpen;

    if (this.isOpen) {
      this.scrollToBottom()
      this.chatbotService.addBotMessage('Chào bạn! Mình là Eventa – trợ lý ảo luôn sẵn sàng hỗ trợ bạn 24/7 ');
      this.messages = this.chatbotService.getMessages();
    }
  }

  sendMessage() {
    const message = this.userMessage.trim();
    if (!message) return;

    this.chatbotService.addUserMessage(message);
    this.userMessage = '';
    this.isLoading = true;

    this.messages = this.chatbotService.getMessages();

    this.chatbotService.chat(message).subscribe(() => {
      this.messages = this.chatbotService.getMessages();
      this.isLoading = false; 
    });
  }

  private scrollToBottom(): void {
    if (this.chatContainer?.nativeElement) {
      this.chatContainer.nativeElement.scrollTop = this.chatContainer.nativeElement.scrollHeight;
    }
  }

  ngAfterViewChecked() {
    if (this.isOpen) {
      this.scrollToBottom();
    }
  }

  toggleSuggestions() {
    this.showSuggestions = !this.showSuggestions;
  }
  suggestedQuestions: string[] = [
    'Sự kiện nào đang được mở bán?',
    'Làm sao để mua vé?',
    'Tôi có thể thanh toán bằng phương thức nào?',
    'Sau khi mua vé xong tôi sẽ nhận được vé như thế nào?',
    'Tôi không nhận được vé thì phải làm sao?'
  ];
  
  sendSuggestedQuestion(question: string) {
    this.userMessage = question;
    this.sendMessage(); // Gửi câu hỏi như người dùng tự gõ
    this.showSuggestions = false;
  }

}
