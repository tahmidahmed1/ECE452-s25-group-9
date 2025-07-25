"""add_message_enhancements

Revision ID: 009
Revises: 008
Create Date: 2025-07-23 08:37:30.000000

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '009'
down_revision = '008'
branch_labels = None
depends_on = None


def upgrade():
    # Add new columns to messages table
    op.add_column('messages', sa.Column('is_read', sa.Boolean(), nullable=False, server_default='false'))
    op.add_column('messages', sa.Column('is_important_sender', sa.Boolean(), nullable=False, server_default='false'))
    op.add_column('messages', sa.Column('is_important_receiver', sa.Boolean(), nullable=False, server_default='false'))
    op.add_column('messages', sa.Column('is_deleted_sender', sa.Boolean(), nullable=False, server_default='false'))
    op.add_column('messages', sa.Column('is_deleted_receiver', sa.Boolean(), nullable=False, server_default='false'))
    
    # Add foreign key constraints to messages table
    op.create_foreign_key('fk_messages_sender_id', 'messages', 'users', ['sender_id'], ['id'], ondelete='CASCADE')
    op.create_foreign_key('fk_messages_receiver_id', 'messages', 'users', ['receiver_id'], ['id'], ondelete='CASCADE')
    
    # Create message_reactions table
    op.create_table('message_reactions',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('message_id', sa.Integer(), nullable=False),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('emoji', sa.String(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['message_id'], ['messages.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_message_reactions_id'), 'message_reactions', ['id'], unique=False)
    
    # Add unique constraint to prevent duplicate reactions
    op.create_unique_constraint('uq_message_reactions_message_user_emoji', 'message_reactions', ['message_id', 'user_id', 'emoji'])


def downgrade():
    # Drop message_reactions table
    op.drop_constraint('uq_message_reactions_message_user_emoji', 'message_reactions', type_='unique')
    op.drop_index(op.f('ix_message_reactions_id'), table_name='message_reactions')
    op.drop_table('message_reactions')
    
    # Drop foreign key constraints from messages table
    op.drop_constraint('fk_messages_receiver_id', 'messages', type_='foreignkey')
    op.drop_constraint('fk_messages_sender_id', 'messages', type_='foreignkey')
    
    # Drop new columns from messages table
    op.drop_column('messages', 'is_deleted_receiver')
    op.drop_column('messages', 'is_deleted_sender')
    op.drop_column('messages', 'is_important_receiver')
    op.drop_column('messages', 'is_important_sender')
    op.drop_column('messages', 'is_read')
